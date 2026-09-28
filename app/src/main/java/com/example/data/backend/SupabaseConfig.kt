package com.example.data.backend

object SupabaseConfig {
    // Supabase project coordinates for Quran Academy.
    // The anon key is meant to be public (Supabase's model relies on RLS, not key secrecy) -
    // it must never be granted write access on its own; see supabase/migrations for the
    // policies that actually gate what an anon vs authenticated request can do.
    var projectUrl: String = "https://amcdmiioovbhzkjqxdrk.supabase.co"
    var anonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFtY2RtaWlvb3ZiaHpranF4ZHJrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTAzNTU1ODUsImV4cCI6MjEwNTkzMTU4NX0.sSLLbeVJnGKdW8491p_z9IssJJsWM1tWDS5gOwOvGxY"

    // LiveKit Cloud WebSocket URL. The API key/secret pair is intentionally NOT stored here
    // anymore - token minting happens exclusively in the livekit-token edge function, which
    // reads its own secret from its server-side environment. A client should never hold it.
    var liveKitServerUrl: String = "wss://quran-accadmy-w23cv7zm.livekit.cloud"
    var liveKitApiKey: String = "APIHUMTB5KGJXq4"
    var liveKitBackendAuthUrl: String = "https://amcdmiioovbhzkjqxdrk.supabase.co/functions/v1/livekit-token"

    var isConfigured: Boolean = true

    fun hasCustomSupabase(): Boolean {
        return projectUrl.isNotBlank() && !projectUrl.contains("e5vqv7mfcfvyi63sfjndxr")
    }
}

/**
 * Holds the current Supabase Auth session in memory. Every authenticated backend request
 * must send [accessToken] as its Authorization bearer so Postgres RLS policies can resolve
 * auth.uid()/auth.email() for that specific user - sending only the anon key (the old
 * behavior) makes every request indistinguishable from an anonymous visitor.
 */
object SupabaseSession {
    @Volatile var accessToken: String = ""
    @Volatile var refreshToken: String = ""
    @Volatile var userId: String = ""

    val isSignedIn: Boolean get() = accessToken.isNotBlank()

    /** Authorization bearer to send with backend requests: the user's JWT once signed in. */
    fun bearerToken(): String = accessToken.ifBlank { SupabaseConfig.anonKey }

    fun set(accessToken: String, refreshToken: String, userId: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.userId = userId
    }

    fun clear() {
        accessToken = ""
        refreshToken = ""
        userId = ""
    }
}
