package com.example.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log

enum class DeviceTier {
    HIGH, LOW
}

object DevicePerformanceMonitor {
    private const val TAG = "DevicePerfMonitor"

    /**
     * Industry Standard Device Profiling with Thermal & Battery Throttling Guard.
     * Determines hardware capability tier based on RAM, CPU Cores, OS age, and Thermal state.
     */
    fun getDeviceTier(context: Context): DeviceTier {
        // 1. Check Battery Saver Mode
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager?.isPowerSaveMode == true) {
            Log.i(TAG, "Battery saver active -> forced LOW tier")
            return DeviceTier.LOW
        }

        // 2. Check Android 10+ Thermal Status
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            val thermalStatus = powerManager.currentThermalStatus
            if (thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE) {
                Log.w(TAG, "Thermal status SEVERE ($thermalStatus) -> forced LOW tier to prevent overheating")
                return DeviceTier.LOW
            }
        }

        // 3. Hardware RAM and Core Capacity
        val cores = Runtime.getRuntime().availableProcessors()
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        var ramGb = 0.0
        if (actManager != null) {
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            ramGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        }

        // Standard: > 3.5GB RAM and 6+ cores on Android 11+ without thermal pressure is High Tier.
        return if (ramGb > 3.5 && cores >= 6 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            DeviceTier.HIGH
        } else {
            DeviceTier.LOW
        }
    }
}
