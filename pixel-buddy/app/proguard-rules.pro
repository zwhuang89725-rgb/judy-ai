# Pixel Buddy ProGuard Rules

# ── Retrofit ──
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# ── Gson ──
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.pixelbuddy.app.data.remote.** { <fields>; }
-keep class com.pixelbuddy.app.domain.model.** { <fields>; }
-keep class com.google.gson.** { *; }

# ── OkHttp ──
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ── Room ──
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { <fields>; }
-dontwarn androidx.room.paging.**

# ── Hilt / Dagger ──
-dontwarn dagger.**
-keep class dagger.** { *; }

# ── Coroutines ──
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── Media3 / ExoPlayer ──
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── Compose ──
-keep class androidx.compose.** { *; }

# ── App models (for Gson serialization) ──
-keep class com.pixelbuddy.app.domain.model.ChatMessage { *; }
-keep class com.pixelbuddy.app.domain.model.Story { *; }
-keep class com.pixelbuddy.app.data.local.ChatMessageEntity { *; }
-keep class com.pixelbuddy.app.data.local.ModelConfigEntity { *; }
