package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class LiveKitCallService : Service() {

    companion object {
        private const val CHANNEL_ID = "LiveCallChannel"
        private const val NOTIFICATION_ID = 1002
        private const val ACTION_START = "ACTION_START_CALL_SERVICE"
        private const val ACTION_STOP = "ACTION_STOP_CALL_SERVICE"

        fun startService(context: Context, className: String) {
            val intent = Intent(context, LiveKitCallService::class.java).apply {
                action = ACTION_START
                putExtra("className", className)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LiveKitCallService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val className = intent.getStringExtra("className") ?: "Quran Class"
                createNotificationChannel()
                
                // Note: Ensure res/drawable/ic_launcher_foreground.xml exists, otherwise use a safe system icon.
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Live Quran Class Active")
                    .setContentText("Connected to $className")
                    .setSmallIcon(android.R.drawable.ic_menu_camera)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOngoing(true)
                    .build()
                
                // Android 14+ requires service type in manifest and startForeground
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
            ACTION_STOP -> {
                stopForeground(true)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Call Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the LiveKit WebRTC call active in the background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
