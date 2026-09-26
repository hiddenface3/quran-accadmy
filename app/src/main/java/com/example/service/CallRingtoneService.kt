package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

/**
 * Foreground Call Ringing Service for Quran Academy Connect.
 * Plays incoming call ringtone, vibrates device, and launches full-screen intent
 * even when the device is locked or screen is off.
 */
class CallRingtoneService : Service() {

    companion object {
        private const val TAG = "CallRingtoneService"
        const val CHANNEL_ID = "quran_academy_incoming_calls"
        const val NOTIFICATION_ID = 9999

        const val ACTION_START_RING = "com.example.action.START_RING"
        const val ACTION_STOP_RING = "com.example.action.STOP_RING"
        const val ACTION_DECLINE_CALL = "com.example.action.DECLINE_CALL"

        const val EXTRA_TEACHER_NAME = "extra_teacher_name"
        const val EXTRA_STUDENT_NAME = "extra_student_name"
        const val EXTRA_CLASS_ID = "extra_class_id"
        const val EXTRA_ROOM_NAME = "extra_room_name"

        fun startRinging(
            context: Context,
            teacherName: String,
            studentName: String,
            classId: String,
            roomName: String
        ) {
            val intent = Intent(context, CallRingtoneService::class.java).apply {
                action = ACTION_START_RING
                putExtra(EXTRA_TEACHER_NAME, teacherName)
                putExtra(EXTRA_STUDENT_NAME, studentName)
                putExtra(EXTRA_CLASS_ID, classId)
                putExtra(EXTRA_ROOM_NAME, roomName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopRinging(context: Context) {
            val intent = Intent(context, CallRingtoneService::class.java).apply {
                action = ACTION_STOP_RING
            }
            context.startService(intent)
        }
    }

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RING -> {
                val teacherName = intent.getStringExtra(EXTRA_TEACHER_NAME) ?: "Sheikh Abdullah"
                val studentName = intent.getStringExtra(EXTRA_STUDENT_NAME) ?: "Student"
                val classId = intent.getStringExtra(EXTRA_CLASS_ID) ?: ""
                val roomName = intent.getStringExtra(EXTRA_ROOM_NAME) ?: ""

                startForegroundNotification(teacherName, studentName, classId, roomName)
                startRingtoneAndVibration()
            }
            ACTION_DECLINE_CALL, ACTION_STOP_RING -> {
                stopRingtoneAndVibration()
                stopForeground(true)
                stopSelf()
            }
            else -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundNotification(
        teacherName: String,
        studentName: String,
        classId: String,
        roomName: String
    ) {
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CLASS_ID, classId)
            putExtra(EXTRA_ROOM_NAME, roomName)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = Intent(this, CallRingtoneService::class.java).apply {
            action = ACTION_DECLINE_CALL
        }
        val declinePendingIntent = PendingIntent.getService(
            this,
            1,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Incoming Quran Live Class Call")
            .setContentText("$teacherName is calling $studentName for Live Session")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "Answer Class", fullScreenPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", declinePendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startRingtoneAndVibration() {
        try {
            val ringtoneUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(applicationContext, ringtoneUri)
            ringtone?.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone?.play()
        } catch (e: Exception) {
            Log.w(TAG, "Ringtone play error: ${e.message}")
        }

        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mgr = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                mgr?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            val pattern = longArrayOf(0, 1000, 1000, 1000, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibrator error: ${e.message}")
        }
    }

    private fun stopRingtoneAndVibration() {
        try {
            ringtone?.stop()
            ringtone = null
        } catch (_: Exception) {}

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Quran Academy Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live 1-on-1 Quran tutoring incoming call alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 1000, 1000)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopRingtoneAndVibration()
        super.onDestroy()
    }
}
