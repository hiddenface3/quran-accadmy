package com.example.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build

enum class DeviceTier {
    HIGH, LOW
}

object DevicePerformanceMonitor {
    /**
     * Industry Standard Device Profiling
     * Determines hardware capability tier based on RAM, CPU Cores, and OS age.
     */
    fun getDeviceTier(context: Context): DeviceTier {
        val cores = Runtime.getRuntime().availableProcessors()
        
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        var ramGb = 0.0
        if (actManager != null) {
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            ramGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        }

        // Standard: > 3.5GB RAM and 6+ cores on Android 11+ is High Tier.
        // Everything else (old Android 9/10, low RAM, low cores) is Low Tier.
        return if (ramGb > 3.5 && cores >= 6 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            DeviceTier.HIGH
        } else {
            DeviceTier.LOW
        }
    }
}
