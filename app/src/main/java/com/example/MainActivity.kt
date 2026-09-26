package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.QuranAcademyApp
import com.example.ui.theme.QuranAcademyTheme

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.livekit.LiveCallEngine

class MainActivity : ComponentActivity() {
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupNetworkMonitoring()
        setContent {
            QuranAcademyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: MainViewModel = viewModel()
                    QuranAcademyApp(viewModel = viewModel)
                }
            }
        }
    }

    private fun setupNetworkMonitoring() {
        try {
            connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    LiveCallEngine.onNetworkChanged(true)
                }

                override fun onLost(network: Network) {
                    LiveCallEngine.onNetworkChanged(false)
                }
            }
            networkCallback?.let {
                connectivityManager?.registerNetworkCallback(request, it)
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
    }
}
