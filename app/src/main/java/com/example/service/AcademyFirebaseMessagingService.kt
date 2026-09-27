package com.example.service

import android.util.Log
import com.example.data.local.AppPreferences
import com.example.data.model.UserRole
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch

/**
 * Handles incoming Firebase Cloud Messaging high-priority push events
 * to wake up device and ring when the app is closed, backgrounded, or screen is off.
 */
class AcademyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "AcademyFCM"
        const val TOPIC_CALLS = "quran_incoming_calls"

        fun initializeTopics(prefs: AppPreferences) {
            try {
                FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_CALLS)
                val user = prefs.getSavedUser()
                if (user != null) {
                    val sanitizedId = user.id.replace(Regex("[^a-zA-Z0-9-_.~%]+"), "_")
                    if (sanitizedId.isNotBlank()) {
                        FirebaseMessaging.getInstance().subscribeToTopic("user_$sanitizedId")
                    }
                    val sanitizedName = user.name.lowercase().trim().replace(Regex("[^a-zA-Z0-9-_.~%]+"), "_")
                    if (sanitizedName.isNotBlank()) {
                        FirebaseMessaging.getInstance().subscribeToTopic("student_$sanitizedName")
                    }
                }
                Log.i(TAG, "Subscribed to FCM call topics successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Topic subscription error: ${e.message}")
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New FCM Device Token generated: $token")
        val prefs = AppPreferences(applicationContext)
        prefs.saveFcmToken(token)
        initializeTopics(prefs)

        val user = prefs.getSavedUser()
        if (user != null && user.id.isNotBlank()) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val backend = com.example.data.backend.AcademyBackendService()
                backend.updateUserFcmToken(user.id, token)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "FCM Push Received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isEmpty()) {
            Log.d(TAG, "FCM data payload is empty")
            return
        }

        val action = data["action"] ?: ""
        val classId = data["class_id"] ?: ""
        val teacherName = data["teacher_name"] ?: "Quran Teacher"
        val studentName = data["student_name"] ?: ""
        val roomName = data["room_name"] ?: "room_$classId"

        Log.i(TAG, "FCM Action: $action, classId: $classId, student: $studentName")

        when (action) {
            "INCOMING_CALL" -> {
                val prefs = AppPreferences(applicationContext)
                val currentUser = prefs.getSavedUser()

                // Filter: if user is student, verify target name matches
                if (currentUser != null && currentUser.role == UserRole.STUDENT && studentName.isNotBlank()) {
                    val cleanTarget = studentName.trim().lowercase()
                    val cleanCurrent = currentUser.name.trim().lowercase()
                    val isMatch = cleanTarget == cleanCurrent ||
                            cleanTarget.contains(cleanCurrent) ||
                            cleanCurrent.contains(cleanTarget)

                    if (!isMatch) {
                        Log.d(TAG, "Call ignored: intended for $studentName, current user is ${currentUser.name}")
                        return
                    }
                }

                // NEW: Prevent self-receiving call (teacher calls student, but gets the push themselves)
                if (currentUser != null && currentUser.name.trim().lowercase() == teacherName.trim().lowercase()) {
                    Log.d(TAG, "Call ignored: initiated by myself (${currentUser.name})")
                    return
                }

                // Trigger ringing service with full-screen intent & wake lock
                CallRingtoneService.startRinging(
                    context = applicationContext,
                    teacherName = teacherName,
                    studentName = studentName.ifBlank { currentUser?.name ?: "Student" },
                    classId = classId,
                    roomName = roomName
                )
            }
            "CANCEL_CALL", "DISMISS_CALL" -> {
                CallRingtoneService.stopRinging(applicationContext)
            }
        }
    }
}
