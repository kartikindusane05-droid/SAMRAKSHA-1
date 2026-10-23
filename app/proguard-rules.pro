# SAMRAKSHA Production Proguard & R8 Optimization Rules

# Compose
-dontwarn androidx.compose.**

# Data & Domain Models
-keep class com.samraksha.app.data.model.** { *; }
-keepclassmembers class com.samraksha.app.data.** {
    *;
}
-keepclassmembers class com.samraksha.app.domain.** {
    *;
}

# Gson & Serialization
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit & OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**

# MapLibre Native SDK
-keep class org.maplibre.android.** { *; }
-dontwarn org.maplibre.android.**
-keepclasseswithmembernames class * {
    native <methods>;
}

# Coroutines & Room
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
