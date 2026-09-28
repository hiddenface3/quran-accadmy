package com.example.data.backend

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Talks to Supabase's GoTrue Auth REST API directly (no extra SDK dependency, consistent
 * with the rest of this codebase's OkHttp-based backend calls).
 *
 * This is the ONLY source of truth for "who is this user" - unlike the old flow, a role or
 * identity claimed here is verified server-side (password check / Google ID token signature),
 * not just typed into a form field.
 */
class SupabaseAuthService {

    companion object {
        private const val TAG = "SupabaseAuth"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    data class AuthSession(
        val accessToken: String,
        val refreshToken: String,
        val userId: String,
        val email: String
    )

    /** Creates a new Supabase Auth user. Depending on project settings this may require the
     * user to confirm their email before a session is returned (result.session == null). */
    suspend fun signUp(email: String, password: String, fullName: String): Result<AuthSession?> =
        withContext(Dispatchers.IO) {
            try {
                val url = "${SupabaseConfig.projectUrl}/auth/v1/signup"
                val json = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                    put("data", JSONObject().apply { put("full_name", fullName) })
                }
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.anonKey)
                    .addHeader("Content-Type", "application/json")
                    .post(json.toString().toRequestBody(JSON_MEDIA))
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: "{}"
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception(parseAuthError(body)))
                    }
                    Result.success(parseSession(JSONObject(body)))
                }
            } catch (e: Exception) {
                Log.w(TAG, "signUp error: ${e.message}")
                Result.failure(e)
            }
        }

    suspend fun signInWithPassword(email: String, password: String): Result<AuthSession> =
        withContext(Dispatchers.IO) {
            try {
                val url = "${SupabaseConfig.projectUrl}/auth/v1/token?grant_type=password"
                val json = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.anonKey)
                    .addHeader("Content-Type", "application/json")
                    .post(json.toString().toRequestBody(JSON_MEDIA))
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: "{}"
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception(parseAuthError(body)))
                    }
                    val session = parseSession(JSONObject(body))
                        ?: return@withContext Result.failure(Exception("Sign-in did not return a session"))
                    Result.success(session)
                }
            } catch (e: Exception) {
                Log.w(TAG, "signInWithPassword error: ${e.message}")
                Result.failure(e)
            }
        }

    /** Exchanges a verified Google ID token (from Credential Manager) for a Supabase session.
     * Requires the Google provider to be enabled under Authentication > Providers in the
     * Supabase dashboard - that one step can't be done from application code. */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthSession> =
        withContext(Dispatchers.IO) {
            try {
                val url = "${SupabaseConfig.projectUrl}/auth/v1/token?grant_type=id_token"
                val json = JSONObject().apply {
                    put("provider", "google")
                    put("id_token", idToken)
                }
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.anonKey)
                    .addHeader("Content-Type", "application/json")
                    .post(json.toString().toRequestBody(JSON_MEDIA))
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: "{}"
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception(parseAuthError(body)))
                    }
                    val session = parseSession(JSONObject(body))
                        ?: return@withContext Result.failure(Exception("Google sign-in did not return a session"))
                    Result.success(session)
                }
            } catch (e: Exception) {
                Log.w(TAG, "signInWithGoogleIdToken error: ${e.message}")
                Result.failure(e)
            }
        }

    suspend fun refreshSession(refreshToken: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.projectUrl}/auth/v1/token?grant_type=refresh_token"
            val json = JSONObject().apply { put("refresh_token", refreshToken) }
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Content-Type", "application/json")
                .post(json.toString().toRequestBody(JSON_MEDIA))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: "{}"
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception(parseAuthError(body)))
                }
                val session = parseSession(JSONObject(body))
                    ?: return@withContext Result.failure(Exception("Refresh did not return a session"))
                Result.success(session)
            }
        } catch (e: Exception) {
            Log.w(TAG, "refreshSession error: ${e.message}")
            Result.failure(e)
        }
    }

    /** Server-verified profile fetch: uses the caller's own access token, so RLS only ever
     * returns their own row (see the "Users read own profile" policy in the migration). */
    suspend fun fetchOwnProfile(accessToken: String, userId: String): Result<JSONObject> =
        withContext(Dispatchers.IO) {
            try {
                val url = "${SupabaseConfig.projectUrl}/rest/v1/profiles?id=eq.$userId&select=*"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.anonKey)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("HTTP ${response.code}"))
                    }
                    val arr = org.json.JSONArray(response.body?.string() ?: "[]")
                    if (arr.length() == 0) {
                        return@withContext Result.failure(Exception("Profile not found yet - it may still be provisioning"))
                    }
                    Result.success(arr.getJSONObject(0))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun signOut(accessToken: String) = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.projectUrl}/auth/v1/logout"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .post("".toRequestBody(null))
                .build()
            client.newCall(request).execute().close()
        } catch (_: Exception) {
            // Best-effort; local session is cleared regardless by the caller.
        }
    }

    private fun parseSession(json: JSONObject): AuthSession? {
        val accessToken = json.optString("access_token")
        if (accessToken.isBlank()) return null
        val refreshToken = json.optString("refresh_token")
        val user = json.optJSONObject("user")
        val userId = user?.optString("id") ?: ""
        val email = user?.optString("email") ?: ""
        if (userId.isBlank()) return null
        return AuthSession(accessToken, refreshToken, userId, email)
    }

    private fun parseAuthError(body: String): String {
        return try {
            val json = JSONObject(body)
            json.optString("error_description", json.optString("msg", json.optString("error", "Authentication failed")))
        } catch (_: Exception) {
            "Authentication failed"
        }
    }
}
