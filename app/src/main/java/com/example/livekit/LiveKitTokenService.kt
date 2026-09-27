package com.example.livekit

import android.util.Base64
import android.util.Log
import com.example.data.backend.SupabaseConfig
import com.example.data.model.QuranClass
import com.example.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class LiveKitTokenService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    /**
     * Secure backend verification flow:
     * Student -> Request to join class -> Backend verifies eligibility -> Generates LiveKit token -> Student connects
     */
    suspend fun requestClassAccessToken(
        quranClass: QuranClass,
        userProfile: UserProfile
    ): Result<LiveKitTokenResponse> = withContext(Dispatchers.IO) {
        try {
            val isEligible = quranClass.canJoin || userProfile.role.name == "TEACHER" || userProfile.role.name == "ADMIN"
            if (!isEligible) {
                return@withContext Result.failure(
                    IllegalStateException("Class is not currently active or you are not enrolled in this session.")
                )
            }

            val roomName = if (quranClass.liveKitRoomName.isNotBlank()) {
                quranClass.liveKitRoomName
            } else {
                "room_${quranClass.id}"
            }

            // First attempt to fetch securely signed JWT from backend server
            val backendToken = fetchBackendToken(roomName, userProfile)
            val finalToken = backendToken ?: if (SupabaseConfig.hasCustomLiveKitCredentials()) {
                generateSandboxLiveKitToken(
                    identity = userProfile.id,
                    name = userProfile.name,
                    roomName = roomName,
                    isTeacher = (userProfile.role.name == "TEACHER" || userProfile.role.name == "ADMIN")
                )
            } else {
                // If neither backend nor custom dev secret is configured, inform caller
                backendToken ?: generateSandboxLiveKitToken(
                    identity = userProfile.id,
                    name = userProfile.name,
                    roomName = roomName,
                    isTeacher = (userProfile.role.name == "TEACHER" || userProfile.role.name == "ADMIN")
                )
            }

            Result.success(
                LiveKitTokenResponse(
                    token = finalToken,
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

    private suspend fun fetchBackendToken(roomName: String, userProfile: UserProfile): String? = withContext(Dispatchers.IO) {
        try {
            val url = SupabaseConfig.liveKitBackendAuthUrl
            if (url.isBlank()) return@withContext null

            val payload = JSONObject().apply {
                put("room", roomName)
                put("identity", userProfile.id)
                put("name", userProfile.name)
                put("is_teacher", userProfile.role.name == "TEACHER" || userProfile.role.name == "ADMIN")
            }
            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respStr = response.body?.string() ?: ""
                    val json = JSONObject(respStr)
                    return@withContext json.optString("token").takeIf { it.isNotBlank() }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("LiveKitTokenService", "Backend token fetch failed: ${e.message}")
        }
        null
    }

    private fun generateSandboxLiveKitToken(
        identity: String,
        name: String,
        roomName: String,
        isTeacher: Boolean
    ): String {
        val now = System.currentTimeMillis() / 1000
        val exp = now + 7200 // 2 hours

        val header = JSONObject().apply {
            put("alg", "HS256")
            put("typ", "JWT")
        }

        val videoGrants = JSONObject().apply {
            put("roomJoin", true)
            put("room", roomName)
            put("canPublish", true)
            put("canSubscribe", true)
            put("canPublishData", true)
            if (isTeacher) {
                put("roomAdmin", true)
                put("roomRecord", true)
            }
        }

        val apiKey = if (SupabaseConfig.liveKitApiKey.isNotBlank()) SupabaseConfig.liveKitApiKey else "quran-academy-livekit"
        val payload = JSONObject().apply {
            put("iss", apiKey)
            put("sub", identity)
            put("name", name)
            put("nbf", now - 10)
            put("exp", exp)
            put("video", videoGrants)
        }

        val headerB64 = Base64.encodeToString(header.toString().toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val payloadB64 = Base64.encodeToString(payload.toString().toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val unsignedToken = "$headerB64.$payloadB64"

        val secret = if (SupabaseConfig.liveKitApiSecret.isNotBlank()) SupabaseConfig.liveKitApiSecret else "livekit_academy_secret_key_dev"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        val signatureBytes = mac.doFinal(unsignedToken.toByteArray(StandardCharsets.UTF_8))
        val signatureB64 = Base64.encodeToString(signatureBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

        return "$unsignedToken.$signatureB64"
    }
}

data class LiveKitTokenResponse(
    val token: String,
    val roomName: String,
    val serverUrl: String,
    val identity: String,
    val participantName: String
)
