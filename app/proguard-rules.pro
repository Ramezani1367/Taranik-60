# ========== Mp3agic (ID3 Tag Library) ==========
-keep class com.mpatric.mp3agic.** { *; }
-dontwarn com.mpatric.mp3agic.**

# ========== Hilt ==========
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }

# ========== Coroutines ==========
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile **;
}

# ========== Compose ==========
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# ========== Retrofit/OkHttp ==========
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# ========== DataStore ==========
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# ========== AndroidX ==========
-keep class androidx.startup.** { *; }
-keep class androidx.datastore.** { *; }

# ========== General ==========
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keepclassmembers class * {
    *** getContext();
    *** getActivity();
}
