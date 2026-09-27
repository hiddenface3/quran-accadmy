package com.example.data.backend

object SupabaseConfig {
    // Supabase project coordinates for Quran Academy
    var projectUrl: String = "https://amcdmiioovbhzkjqxdrk.supabase.co"
    var anonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFtY2RtaWlvb3ZiaHpranF4ZHJrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTAzNTU1ODUsImV4cCI6MjEwNTkzMTU4NX0.sSLLbeVJnGKdW8491p_z9IssJJsWM1tWDS5gOwOvGxY"
    
    // LiveKit Cloud credentials & WebSocket URL
    var liveKitServerUrl: String = "wss://quran-accadmy-w23cv7zm.livekit.cloud"
    var liveKitApiKey: String = "APIHUMTB5KGJXq4"
    var liveKitApiSecret: String = ""
    var liveKitBackendAuthUrl: String = "https://amcdmiioovbhzkjqxdrk.supabase.co/functions/v1/livekit-token"
    
    var isConfigured: Boolean = true

    fun hasCustomLiveKitCredentials(): Boolean {
        return liveKitApiKey.isNotBlank() && liveKitApiKey != "devkey" && liveKitApiSecret.isNotBlank() && liveKitApiSecret != "secret"
    }

    fun hasCustomSupabase(): Boolean {
        return projectUrl.isNotBlank() && !projectUrl.contains("e5vqv7mfcfvyi63sfjndxr")
    }
}
