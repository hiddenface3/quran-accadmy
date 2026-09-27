package com.example.service

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.concurrent.TimeUnit

/**
 * Sends high-priority Firebase Cloud Messaging (FCM HTTP v1) push notifications
 * using the Firebase Admin Service Account.
 */
object FcmNotificationSender {

    private const val TAG = "FcmNotificationSender"
    private const val PROJECT_ID = "quran-academy-d6ff0"
    private const val CLIENT_EMAIL = "firebase-adminsdk-fbsvc@quran-academy-d6ff0.iam.gserviceaccount.com"
    private const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
    private const val FCM_ENDPOINT = "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send"

    // Private key is securely loaded at runtime from environment or backend configuration.
    // Master service account private keys are NEVER compiled into client APKs.
    private var customServiceAccountJson: String? = null

    fun setServiceAccountConfig(json: String) {
        customServiceAccountJson = json
    }

    private fun getPrivateKeyPem(): String {
        return try {
            val jsonStr = customServiceAccountJson ?: ""
            if (jsonStr.isBlank()) return ""
            val json = JSONObject(jsonStr)
            json.getString("private_key")
        } catch (_: Exception) {
            ""
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private var cachedAccessToken: String? = null
    private var tokenExpiryTime: Long = 0

    /**
     * Get or refresh Google OAuth2 access token for FCM HTTP v1
     */
    private suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedAccessToken != null && now < tokenExpiryTime - 60_000) {
            return@withContext cachedAccessToken
        }

        try {
            val keyPem = getPrivateKeyPem()
            if (keyPem.isBlank()) {
                Log.e(TAG, "Private key is blank")
                return@withContext null
            }
            val jwt = createSignedJwt(CLIENT_EMAIL, keyPem)

            val formBody = FormBody.Builder()
                .add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                .add("assertion", jwt)
                .build()

            val request = Request.Builder()
                .url(TOKEN_ENDPOINT)
                .post(formBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val token = json.getString("access_token")
                    val expiresIn = json.optLong("expires_in", 3600)
                    cachedAccessToken = token
                    tokenExpiryTime = now + (expiresIn * 1000)
                    Log.i(TAG, "Obtained fresh Google OAuth2 access token for FCM")
                    return@withContext token
                } else {
                    Log.e(TAG, "Failed to get Google OAuth2 token: $bodyStr")
                    return@withContext null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating Google OAuth2 token: ${e.message}", e)
            return@withContext null
        }
    }

    private fun createSignedJwt(email: String, privateKeyPem: String): String {
        val headerJson = JSONObject().apply {
            put("alg", "RS256")
            put("typ", "JWT")
        }.toString()

        val nowSeconds = System.currentTimeMillis() / 1000
        val payloadJson = JSONObject().apply {
            put("iss", email)
            put("scope", "https://www.googleapis.com/auth/firebase.messaging")
            put("aud", TOKEN_ENDPOINT)
            put("exp", nowSeconds + 3600)
            put("iat", nowSeconds)
        }.toString()

        val encodedHeader = Base64.encodeToString(
            headerJson.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        val encodedPayload = Base64.encodeToString(
            payloadJson.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        val signingInput = "$encodedHeader.$encodedPayload"

        val cleanKey = privateKeyPem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s+".toRegex(), "")
        val keyBytes = Base64.decode(cleanKey, Base64.DEFAULT)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKey = keyFactory.generatePrivate(keySpec)

        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(privateKey)
        signer.update(signingInput.toByteArray(StandardCharsets.UTF_8))
        val signatureBytes = signer.sign()

        val encodedSignature = Base64.encodeToString(
            signatureBytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

        return "$signingInput.$encodedSignature"
    }

    /**
     * Dispatch high-priority incoming call push to wake up student's phone
     * Uses direct 1-to-1 FCM device token targeting (Industry Standard)
     */
    suspend fun sendIncomingCallPush(
        classId: String,
        teacherName: String,
        studentName: String,
        roomName: String,
        targetDeviceToken: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken()
        if (accessToken == null) {
            // Secure industry standard: dispatch push through backend Edge Function
            return@withContext sendBackendCallPush(
                action = "INCOMING_CALL",
                classId = classId,
                teacherName = teacherName,
                studentName = studentName,
                roomName = roomName,
                targetDeviceToken = targetDeviceToken
            )
        }

        try {
            val messageObj = JSONObject().apply {
                if (targetDeviceToken.isNotBlank()) {
                    put("token", targetDeviceToken)
                    Log.i(TAG, "Targeting direct 1-to-1 FCM device token: ${targetDeviceToken.take(16)}...")
                } else {
                    put("topic", AcademyFirebaseMessagingService.TOPIC_CALLS)
                    Log.w(TAG, "Target device token not found for $studentName; falling back to topic push")
                }
                put("data", JSONObject().apply {
                    put("action", "INCOMING_CALL")
                    put("class_id", classId)
                    put("teacher_name", teacherName)
                    put("student_name", studentName)
                    put("room_name", roomName)
                })
                put("android", JSONObject().apply {
                    put("priority", "HIGH")
                })
            }

            val rootObj = JSONObject().apply {
                put("message", messageObj)
            }

            val requestBody = rootObj.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(FCM_ENDPOINT)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json; UTF-8")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Log.i(TAG, "FCM Incoming Call Push successfully dispatched: $respBody")
                    true
                } else {
                    Log.e(TAG, "FCM Push dispatch error (${response.code}): $respBody")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "FCM Push exception: ${e.message}", e)
            false
        }
    }

    /**
     * Send cancellation push when teacher hangs up or call is dismissed
     */
    suspend fun sendCancelCallPush(
        classId: String,
        targetDeviceToken: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken()
        if (accessToken == null) {
            return@withContext sendBackendCallPush(
                action = "CANCEL_CALL",
                classId = classId,
                teacherName = "",
                studentName = "",
                roomName = "",
                targetDeviceToken = targetDeviceToken
            )
        }

        try {
            val messageObj = JSONObject().apply {
                if (targetDeviceToken.isNotBlank()) {
                    put("token", targetDeviceToken)
                } else {
                    put("topic", AcademyFirebaseMessagingService.TOPIC_CALLS)
                }
                put("data", JSONObject().apply {
                    put("action", "CANCEL_CALL")
                    put("class_id", classId)
                })
                put("android", JSONObject().apply {
                    put("priority", "HIGH")
                })
            }

            val rootObj = JSONObject().apply {
                put("message", messageObj)
            }

            val requestBody = rootObj.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(FCM_ENDPOINT)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json; UTF-8")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.i(TAG, "FCM Cancel Call Push dispatched successfully")
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "FCM Cancel Call exception: ${e.message}", e)
            false
        }
    }

    /**
     * Secure backend proxy dispatch via Supabase Edge Function
     */
    private suspend fun sendBackendCallPush(
        action: String,
        classId: String,
        teacherName: String,
        studentName: String,
        roomName: String,
        targetDeviceToken: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${com.example.data.backend.SupabaseConfig.projectUrl}/functions/v1/send-call-push"
            val payload = JSONObject().apply {
                put("action", action)
                put("class_id", classId)
                put("teacher_name", teacherName)
                put("student_name", studentName)
                put("room_name", roomName)
                put("target_token", targetDeviceToken)
            }

            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", com.example.data.backend.SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer ${com.example.data.backend.SupabaseConfig.anonKey}")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code in 200..204) {
                    Log.i(TAG, "Backend Call Push sent successfully ($action)")
                    true
                } else {
                    Log.w(TAG, "Backend Call Push returned code: ${response.code}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Backend push proxy error: ${e.message}")
            false
        }
    }
}
