package com.example.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.backend.SupabaseAuthService
import com.example.data.backend.SupabaseSession
import com.example.data.local.AppPreferences
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Authenticating : AuthState()
    data class Authenticated(val user: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * All identity here is anchored to a real Supabase Auth session (see [SupabaseAuthService]).
 * Role is never decided on-device - it is whatever the server's `profiles.role` column says,
 * which a signed-in user cannot change for themselves (enforced by a DB trigger; see
 * supabase/migrations). A brand-new sign-up always starts as STUDENT; promoting someone to
 * TEACHER/ADMIN is an action only an existing admin can take.
 */
class AuthManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)
    private val authService = SupabaseAuthService()
    val prefs = AppPreferences(context)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Restore a previous session (tokens + cached profile) so the user isn't logged out
        // every app restart. The cached profile is a display convenience only; every backend
        // request still re-verifies via the access token's signature server-side.
        val accessToken = prefs.getAccessToken()
        val refreshToken = prefs.getRefreshToken()
        val userId = prefs.getSupabaseUserId()
        val savedProfile = prefs.getSavedUser()
        if (!accessToken.isNullOrBlank() && !userId.isNullOrBlank() && savedProfile != null) {
            SupabaseSession.set(accessToken, refreshToken.orEmpty(), userId)
            _authState.value = AuthState.Authenticated(savedProfile)
        }
    }

    private suspend fun loadProfile(accessToken: String, userId: String, email: String): Result<UserProfile> {
        return authService.fetchOwnProfile(accessToken, userId).map { json ->
            UserProfile(
                id = userId,
                name = json.optString("name").ifBlank { email.substringBefore("@") },
                email = json.optString("email").ifBlank { email },
                role = try {
                    UserRole.valueOf(json.optString("role", "STUDENT"))
                } catch (_: Exception) {
                    UserRole.STUDENT
                },
                avatarUrl = json.optString("avatar_url"),
                assignedTeacherName = json.optString("assigned_teacher_name"),
                tajweedLevel = json.optString("tajweed_level").ifBlank {
                    "Intermediate Tajweed (Ahkam At-Tilawah)"
                }
            )
        }
    }

    private fun persistSession(session: SupabaseAuthService.AuthSession, profile: UserProfile) {
        SupabaseSession.set(session.accessToken, session.refreshToken, session.userId)
        prefs.saveSession(session.accessToken, session.refreshToken, session.userId)
        prefs.saveUser(profile)
        _authState.value = AuthState.Authenticated(profile)
    }

    suspend fun signUp(name: String, email: String, password: String): Result<UserProfile> {
        _authState.value = AuthState.Authenticating
        val cleanEmail = email.trim().lowercase()
        val signUpResult = authService.signUp(cleanEmail, password, name.trim())
        val session = signUpResult.getOrElse { e ->
            _authState.value = AuthState.Unauthenticated
            return Result.failure(e)
        }
        if (session == null) {
            // Project requires email confirmation before a session is issued.
            _authState.value = AuthState.Unauthenticated
            return Result.failure(Exception("Account created. Please check your email to confirm it, then log in."))
        }
        // The DB trigger that creates the profiles row runs synchronously on signup, so it
        // should already exist; loadProfile still handles the (rare) race gracefully by failing
        // with a clear message rather than fabricating a role.
        return loadProfile(session.accessToken, session.userId, session.email).onSuccess { profile ->
            persistSession(session, profile)
        }.onFailure {
            _authState.value = AuthState.Unauthenticated
        }
    }

    suspend fun signInWithPassword(email: String, password: String): Result<UserProfile> {
        _authState.value = AuthState.Authenticating
        val cleanEmail = email.trim().lowercase()
        val session = authService.signInWithPassword(cleanEmail, password).getOrElse { e ->
            _authState.value = AuthState.Unauthenticated
            return Result.failure(e)
        }
        return loadProfile(session.accessToken, session.userId, session.email).onSuccess { profile ->
            persistSession(session, profile)
        }.onFailure {
            _authState.value = AuthState.Unauthenticated
        }
    }

    suspend fun signInWithGoogle(): Result<UserProfile> {
        _authState.value = AuthState.Authenticating
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("quran-academy-google-oauth.apps.googleusercontent.com")
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(request = request, context = context)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
                val session = authService.signInWithGoogleIdToken(googleIdToken.idToken).getOrElse { e ->
                    _authState.value = AuthState.Unauthenticated
                    return Result.failure(e)
                }
                loadProfile(session.accessToken, session.userId, session.email).onSuccess { profile ->
                    persistSession(session, profile)
                }.onFailure {
                    _authState.value = AuthState.Unauthenticated
                }
            } else {
                _authState.value = AuthState.Unauthenticated
                Result.failure(Exception("Unsupported credential type"))
            }
        } catch (e: GetCredentialCancellationException) {
            _authState.value = AuthState.Unauthenticated
            Result.failure(e)
        } catch (e: GetCredentialException) {
            _authState.value = AuthState.Unauthenticated
            Result.failure(e)
        } catch (e: Exception) {
            _authState.value = AuthState.Unauthenticated
            Result.failure(e)
        }
    }

    fun signOut() {
        val token = SupabaseSession.accessToken
        prefs.clearUser()
        SupabaseSession.clear()
        _authState.value = AuthState.Unauthenticated
        if (token.isNotBlank()) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                authService.signOut(token)
            }
        }
    }
}
