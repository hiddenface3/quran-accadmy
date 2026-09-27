# ProGuard & R8 Optimization Rules for Quran Academy Connect

# Preserve LiveKit and WebRTC native bindings
-keep class io.livekit.android.** { *; }
-keep interface io.livekit.android.** { *; }
-keep class org.webrtc.** { *; }
-keep interface org.webrtc.** { *; }

# Preserve data models used in JSON serialization
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }

# Preserve Room database entities and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Preserve OkHttp and Moshi
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class com.squareup.moshi.** { *; }

# Preserve AndroidX Cryptography
-keep class androidx.security.crypto.** { *; }

# Keep line numbers for error reporting
-keepattributes SourceFile,LineNumberTable
