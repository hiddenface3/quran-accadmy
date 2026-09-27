package com.example.utils

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AudioRoute {
    SPEAKER, EARPIECE, BLUETOOTH
}

/**
 * Industry-Standard Android Audio Routing & Proximity WakeLock Manager.
 * Uses Android 12+ (API 31+) setCommunicationDevice API with fallback for older versions.
 * Integrates PROXIMITY_SCREEN_OFF_WAKE_LOCK to prevent cheek touches during ear-held calls.
 */
object AudioRoutingManager {
    private const val TAG = "AudioRoutingManager"

    private val _currentRoute = MutableStateFlow(AudioRoute.SPEAKER)
    val currentRoute: StateFlow<AudioRoute> = _currentRoute.asStateFlow()

    private val _isBluetoothAvailable = MutableStateFlow(false)
    val isBluetoothAvailable: StateFlow<Boolean> = _isBluetoothAvailable.asStateFlow()

    private var proximityWakeLock: PowerManager.WakeLock? = null
    private var audioDeviceCallback: Any? = null

    fun start(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            
            // Check for Bluetooth devices on startup
            updateAvailableDevices(audioManager)
            
            // Default to Bluetooth if connected, else Speaker
            if (_isBluetoothAvailable.value) {
                setAudioRoute(context, AudioRoute.BLUETOOTH)
            } else {
                setAudioRoute(context, AudioRoute.SPEAKER)
            }

            // Register callback for dynamically plugged/paired devices
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val callback = object : android.media.AudioDeviceCallback() {
                    override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                        updateAvailableDevices(audioManager)
                        val hasBt = addedDevices?.any { isBluetoothDevice(it) } == true
                        if (hasBt) {
                            setAudioRoute(context, AudioRoute.BLUETOOTH)
                        }
                    }

                    override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                        updateAvailableDevices(audioManager)
                        if (!_isBluetoothAvailable.value && _currentRoute.value == AudioRoute.BLUETOOTH) {
                            setAudioRoute(context, AudioRoute.SPEAKER)
                        }
                    }
                }
                audioManager.registerAudioDeviceCallback(callback, null)
                audioDeviceCallback = callback
            }

            // Setup Proximity Wake Lock
            setupProximitySensor(context)
        } catch (e: Exception) {
            Log.w(TAG, "Audio manager start error: ${e.message}")
        }
    }

    fun stop(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager?.clearCommunicationDevice()
            } else {
                @Suppress("DEPRECATION")
                audioManager?.isSpeakerphoneOn = false
                @Suppress("DEPRECATION")
                audioManager?.stopBluetoothSco()
                @Suppress("DEPRECATION")
                audioManager?.isBluetoothScoOn = false
            }

            audioManager?.mode = AudioManager.MODE_NORMAL

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioDeviceCallback != null) {
                audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback as android.media.AudioDeviceCallback)
                audioDeviceCallback = null
            }

            releaseProximitySensor()
        } catch (e: Exception) {
            Log.w(TAG, "Audio manager stop error: ${e.message}")
        }
    }

    fun setAudioRoute(context: Context, route: AudioRoute) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        Log.i(TAG, "Switching audio route to: $route")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val devices = audioManager.availableCommunicationDevices
                val targetDevice = when (route) {
                    AudioRoute.BLUETOOTH -> devices.firstOrNull { isBluetoothDevice(it) }
                    AudioRoute.EARPIECE -> devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
                    AudioRoute.SPEAKER -> devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                }

                if (targetDevice != null) {
                    val success = audioManager.setCommunicationDevice(targetDevice)
                    Log.i(TAG, "setCommunicationDevice(${targetDevice.productName}) = $success")
                } else if (route == AudioRoute.SPEAKER) {
                    audioManager.clearCommunicationDevice()
                }
            } else {
                @Suppress("DEPRECATION")
                when (route) {
                    AudioRoute.BLUETOOTH -> {
                        audioManager.isSpeakerphoneOn = false
                        audioManager.startBluetoothSco()
                        audioManager.isBluetoothScoOn = true
                    }
                    AudioRoute.EARPIECE -> {
                        audioManager.stopBluetoothSco()
                        audioManager.isBluetoothScoOn = false
                        audioManager.isSpeakerphoneOn = false
                    }
                    AudioRoute.SPEAKER -> {
                        audioManager.stopBluetoothSco()
                        audioManager.isBluetoothScoOn = false
                        audioManager.isSpeakerphoneOn = true
                    }
                }
            }
            _currentRoute.value = route

            // Adjust proximity lock: activate when earpiece is used
            if (route == AudioRoute.EARPIECE) {
                acquireProximitySensor()
            } else {
                releaseProximitySensor()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting audio route: ${e.message}")
        }
    }

    private fun updateAvailableDevices(audioManager: AudioManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = audioManager.availableCommunicationDevices
            _isBluetoothAvailable.value = devices.any { isBluetoothDevice(it) }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            _isBluetoothAvailable.value = devices.any { isBluetoothDevice(it) }
        } else {
            @Suppress("DEPRECATION")
            _isBluetoothAvailable.value = audioManager.isBluetoothScoAvailableOffCall
        }
    }

    private fun isBluetoothDevice(device: AudioDeviceInfo): Boolean {
        return device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
               (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && device.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
    }

    private fun setupProximitySensor(context: Context) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null && powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                proximityWakeLock = powerManager.newWakeLock(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "QuranAcademy:ProximityEarGuard"
                ).apply {
                    setReferenceCounted(false)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Proximity setup error: ${e.message}")
        }
    }

    fun acquireProximitySensor() {
        try {
            if (proximityWakeLock?.isHeld == false) {
                proximityWakeLock?.acquire(60 * 60 * 1000L) // 60 minutes safety timeout
                Log.i(TAG, "Proximity ear guard acquired")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Proximity acquire error: ${e.message}")
        }
    }

    fun releaseProximitySensor() {
        try {
            if (proximityWakeLock?.isHeld == true) {
                proximityWakeLock?.release()
                Log.i(TAG, "Proximity ear guard released")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Proximity release error: ${e.message}")
        }
    }
}
