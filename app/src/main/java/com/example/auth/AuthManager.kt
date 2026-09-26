package com.example.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.local.AppPreferences
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Authenticating : AuthState()
    data class Authenticated(val user: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)
    val prefs = AppPreferences(context)

    companion object {
        // Whitelisted Admin accounts: including user's email
        val authorizedAdminEmails = mutableSetOf(
            "swabi5072@gmail.com",
            "admin@quranacademy.com",
            "principal@quranacademy.com",
            "director@quranacademy.com"
        )

        val authorizedTeacherEmails = mutableSetOf(
            "abdullah.sheikh@quranacademy.com",
            "abdullah.mansoor@quranacademy.com",
            "maryam.siddiqui@quranacademy.com",
            "hamza.yusuf@quranacademy.com"
        )

        fun determineRoleForEmail(email: String): UserRole {
            val normalized = email.trim().lowercase()
            return when {
                authorizedAdminEmails.contains(normalized) || normalized.contains("admin") -> UserRole.ADMIN
                authorizedTeacherEmails.contains(normalized) || normalized.contains("teacher") -> UserRole.TEACHER
                else -> UserRole.STUDENT
            }
        }
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Load saved session if exists
        val saved = prefs.getSavedUser()
        if (saved != null) {
            _authState.value = AuthState.Authenticated(saved)
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

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
                val email = googleIdToken.id
                val role = determineRoleForEmail(email)
                val user = UserProfile(
                    id = "usr_${Math.abs(email.hashCode())}",
                    name = googleIdToken.displayName ?: if (role == UserRole.ADMIN) "Admin Khan" else if (role == UserRole.TEACHER) "Quran Teacher" else "Quran Student",
                    email = email,
                    role = role,
                    avatarUrl = googleIdToken.profilePictureUri?.toString() ?: "",
                    assignedTeacherName = ""
                )
                prefs.saveUser(user)
                _authState.value = AuthState.Authenticated(user)
                Result.success(user)
            } else {
                _authState.value = AuthState.Unauthenticated
                Result.failure(Exception("Unsupported credential type"))
            }
        } catch (e: GetCredentialCancellationException) {
            _authState.value = AuthState.Unauthenticated
            Result.failure(e)
        } catch (e: Exception) {
            _authState.value = AuthState.Unauthenticated
            Result.failure(e)
        }
    }

    fun signInWithEmailAndRole(name: String, email: String, chosenRole: UserRole): UserProfile {
        val finalRole = if (authorizedAdminEmails.contains(email.trim().lowercase()) || email.trim().lowercase() == "swabi5072@gmail.com") {
            UserRole.ADMIN
        } else {
            chosenRole
        }
        val cleanName = if (name.isNotBlank()) name.trim() else when (finalRole) {
            UserRole.ADMIN -> "Admin Khan"
            UserRole.TEACHER -> "Quran Teacher"
            UserRole.STUDENT -> "Quran Student"
        }
        val user = UserProfile(
            id = "usr_${Math.abs(email.trim().lowercase().hashCode())}",
            name = cleanName,
            email = email.trim(),
            role = finalRole,
            avatarUrl = "",
            assignedTeacherName = "",
            tajweedLevel = if (finalRole == UserRole.STUDENT) "Intermediate Tajweed (Ahkam At-Tilawah)" else ""
        )
        prefs.saveUser(user)
        _authState.value = AuthState.Authenticated(user)
        return user
    }

    fun signInAsRole(role: UserRole) {
        val user = when (role) {
            UserRole.STUDENT -> UserProfile(
                id = "usr_student_zaidkhan",
                name = "zaidkhan",
                email = "mytest5072@gmail.com",
                role = UserRole.STUDENT,
                assignedTeacherName = "",
                tajweedLevel = "Intermediate Tajweed (Ahkam At-Tilawah)"
            )
            UserRole.TEACHER -> UserProfile(
                id = "usr_teacher_saqib",
                name = "saqib saib",
                email = "itskhan7733@gmail.com",
                role = UserRole.TEACHER
            )
            UserRole.ADMIN -> UserProfile(
                id = "usr_admin_khan",
                name = "Admin Khan",
                email = "swabi5072@gmail.com",
                role = UserRole.ADMIN
            )
        }
        prefs.saveUser(user)
        _authState.value = AuthState.Authenticated(user)
    }

    fun signOut() {
        prefs.clearUser()
        _authState.value = AuthState.Unauthenticated
    }
}
