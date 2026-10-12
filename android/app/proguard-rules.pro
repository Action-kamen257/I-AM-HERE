# Keep Retrofit and Gson data models
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.iamhere.emergency.data.model.** { *; }
-keep class com.iamhere.emergency.sensor.** { *; }
-keep class com.iamhere.emergency.safety.** { *; }

# Keep OkHttp & Retrofit interfaces
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okio.**
-dontwarn okhttp3.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
