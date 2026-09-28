package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.backend.SupabaseConfig
import com.example.data.model.UserProfile
import com.example.data.model.UserRole

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "quran_academy_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.w("AppPreferences", "Secure Keystore fallback to private prefs: ${e.message}")
        context.getSharedPreferences("quran_academy_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_ASSIGNED_TEACHER = "assigned_teacher"
        private const val KEY_TAJWEED_LEVEL = "tajweed_level"
        private const val KEY_AVATAR_URL = "avatar_url"

        private const val KEY_LIVEKIT_URL = "livekit_url"
        private const val KEY_LIVEKIT_KEY = "livekit_key"

        private const val KEY_SUPABASE_URL = "supabase_url"
        private const val KEY_SUPABASE_KEY = "supabase_key"
        private const val KEY_FCM_TOKEN = "fcm_device_token"
        private const val KEY_ACTIVE_CALL_CLASS_ID = "active_call_class_id"

        private const val KEY_SESSION_ACCESS_TOKEN = "session_access_token"
        private const val KEY_SESSION_REFRESH_TOKEN = "session_refresh_token"
        private const val KEY_SESSION_USER_ID = "session_supabase_user_id"
    }

    /**
     * Real Supabase Auth session (JWT access/refresh tokens). This is the source of
     * truth for who the user is on the backend - the local UserProfile cache below is
     * only a UI convenience, never a substitute for a verified server session.
     */
    fun saveSession(accessToken: String, refreshToken: String, supabaseUserId: String) {
        prefs.edit().apply {
            putString(KEY_SESSION_ACCESS_TOKEN, accessToken)
            putString(KEY_SESSION_REFRESH_TOKEN, refreshToken)
            putString(KEY_SESSION_USER_ID, supabaseUserId)
            apply()
        }
    }

    fun getAccessToken(): String? = prefs.getString(KEY_SESSION_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_SESSION_REFRESH_TOKEN, null)
    fun getSupabaseUserId(): String? = prefs.getString(KEY_SESSION_USER_ID, null)

    fun clearSession() {
        prefs.edit().apply {
            remove(KEY_SESSION_ACCESS_TOKEN)
            remove(KEY_SESSION_REFRESH_TOKEN)
            remove(KEY_SESSION_USER_ID)
            apply()
        }
    }

    fun saveFcmToken(token: String) {
        if (token.isNotBlank()) {
            prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
        }
    }

    fun getFcmToken(): String {
        return prefs.getString(KEY_FCM_TOKEN, "") ?: ""
    }

    fun saveUser(user: UserProfile) {
        if (user.fcmToken.isNotBlank()) {
            saveFcmToken(user.fcmToken)
        }
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, user.id)
            putString(KEY_USER_NAME, user.name)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_ROLE, user.role.name)
            putString(KEY_ASSIGNED_TEACHER, user.assignedTeacherName)
            putString(KEY_TAJWEED_LEVEL, user.tajweedLevel)
            putString(KEY_AVATAR_URL, user.avatarUrl)
            apply()
        }
    }

    fun getSavedUser(): UserProfile? {
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            return null
        }
        val id = prefs.getString(KEY_USER_ID, "") ?: ""
        val name = prefs.getString(KEY_USER_NAME, "") ?: ""
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val roleStr = prefs.getString(KEY_USER_ROLE, UserRole.STUDENT.name) ?: UserRole.STUDENT.name
        val role = try {
            UserRole.valueOf(roleStr)
        } catch (e: Exception) {
            UserRole.STUDENT
        }
        val teacher = prefs.getString(KEY_ASSIGNED_TEACHER, "Sheikh Abdullah Al-Mansoor") ?: "Sheikh Abdullah Al-Mansoor"
        val tajweed = prefs.getString(KEY_TAJWEED_LEVEL, "Intermediate Tajweed (Ahkam At-Tilawah)") ?: ""
        val avatar = prefs.getString(KEY_AVATAR_URL, "") ?: ""
        val token = getFcmToken()

        if (name.isBlank() || email.isBlank()) {
            return null
        }

        return UserProfile(
            id = id,
            name = name,
            email = email,
            role = role,
            avatarUrl = avatar,
            assignedTeacherName = teacher,
            tajweedLevel = tajweed,
            fcmToken = token
        )
    }

    fun clearUser() {
        prefs.edit().apply {
            remove(KEY_IS_LOGGED_IN)
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_ROLE)
            remove(KEY_ASSIGNED_TEACHER)
            remove(KEY_TAJWEED_LEVEL)
            remove(KEY_AVATAR_URL)
            apply()
        }
        clearSession()
    }

    fun saveLiveKitConfig(serverUrl: String, apiKey: String) {
        prefs.edit().apply {
            putString(KEY_LIVEKIT_URL, serverUrl)
            putString(KEY_LIVEKIT_KEY, apiKey)
            apply()
        }
        SupabaseConfig.liveKitServerUrl = serverUrl
        SupabaseConfig.liveKitApiKey = apiKey
    }

    fun loadConfigIntoMemory() {
        val lkUrl = prefs.getString(KEY_LIVEKIT_URL, null)
        val lkKey = prefs.getString(KEY_LIVEKIT_KEY, null)
        if (!lkUrl.isNullOrBlank() && !lkUrl.contains("quran-academy.livekit.cloud")) {
            SupabaseConfig.liveKitServerUrl = lkUrl
        }
        if (!lkKey.isNullOrBlank() && lkKey != "devkey") {
            SupabaseConfig.liveKitApiKey = lkKey
        }

        val sbUrl = prefs.getString(KEY_SUPABASE_URL, null)
        val sbKey = prefs.getString(KEY_SUPABASE_KEY, null)
        if (!sbUrl.isNullOrBlank() && !sbUrl.contains("e5vqv7mfcfvyi63sfjndxr")) {
            SupabaseConfig.projectUrl = sbUrl
        }
        if (!sbKey.isNullOrBlank() && !sbKey.contains("default_anon_key")) {
            SupabaseConfig.anonKey = sbKey
        }
    }

    fun saveSupabaseConfig(projectUrl: String, anonKey: String) {
        prefs.edit().apply {
            putString(KEY_SUPABASE_URL, projectUrl)
            putString(KEY_SUPABASE_KEY, anonKey)
            apply()
        }
        SupabaseConfig.projectUrl = projectUrl
        SupabaseConfig.anonKey = anonKey
    }

    fun setActiveCallClassId(classId: String?) {
        if (classId != null) {
            prefs.edit().putString(KEY_ACTIVE_CALL_CLASS_ID, classId).apply()
        } else {
            prefs.edit().remove(KEY_ACTIVE_CALL_CLASS_ID).apply()
        }
    }

    fun getActiveCallClassId(): String? {
        return prefs.getString(KEY_ACTIVE_CALL_CLASS_ID, null)
    }
}
