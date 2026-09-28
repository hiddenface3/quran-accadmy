package com.example.livekit

import android.util.Log
import com.example.data.backend.SupabaseConfig
import com.example.data.backend.SupabaseSession
import com.example.data.model.QuranClass
import com.example.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LiveKitTokenService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /**
     * Secure backend verification flow: the livekit-token edge function forwards our Supabase
     * Auth JWT to PostgREST, resolves our verified profile role and re-checks eligibility
     * against the classes table itself - it never trusts a client-claimed "I'm the teacher"
     * flag. There is intentionally no client-side fallback that signs its own token: a signing
     * secret must never exist on a device.
     */
    suspend fun requestClassAccessToken(
        quranClass: QuranClass,
        userProfile: UserProfile
    ): Result<LiveKitTokenResponse> = withContext(Dispatchers.IO) {
        try {
            val roomName = quranClass.liveKitRoomName.ifBlank { "room_${quranClass.id}" }

            val backendToken = fetchBackendToken(roomName, quranClass.id, userProfile)
                ?: return@withContext Result.failure(
                    IllegalStateException(
                        "Could not reach the classroom server to verify your access. Please check your connection and try again."
                    )
                )

            Result.success(
                LiveKitTokenResponse(
                    token = backendToken,
                    roomName = roomName,
                    serverUrl = SupabaseConfig.liveKitServerUrl,
                    identity = userProfile.id,
                    participantName = userProfile.name
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchBackendToken(roomName: String, classId: String, userProfile: UserProfile): String? =
        withContext(Dispatchers.IO) {
            try {
                val url = SupabaseConfig.liveKitBackendAuthUrl
                if (url.isBlank()) return@withContext null

                val payload = JSONObject().apply {
                    put("room", roomName)
                    put("class_id", classId)
                    put("identity", userProfile.id)
                    put("name", userProfile.name)
                }
                val body = payload.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.anonKey)
                    // The user's own JWT, not the anon key - this is what lets the function
                    // verify who is actually asking and cross-check them against the class.
                    .addHeader("Authorization", "Bearer ${SupabaseSession.bearerToken()}")
                    .post(body)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val respStr = response.body?.string() ?: ""
                        val json = JSONObject(respStr)
                        return@withContext json.optString("token").takeIf { it.isNotBlank() }
                    } else {
                        Log.w("LiveKitTokenService", "Backend token fetch failed: HTTP ${response.code} ${response.body?.string()}")
                    }
                }
            } catch (e: Exception) {
                Log.w("LiveKitTokenService", "Backend token fetch failed: ${e.message}")
            }
            null
        }
}

data class LiveKitTokenResponse(
    val token: String,
    val roomName: String,
    val serverUrl: String,
    val identity: String,
    val participantName: String
)
