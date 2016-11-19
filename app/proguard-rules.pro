##---------------Begin: proguard configuration common for all Android apps ----------
-dontobfuscate
-keepattributes SourceFile,LineNumberTable

-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe

# OkHttp
-keepattributes Signature
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Retrofit
-dontwarn okio.**
-dontwarn retrofit2.Platform$Java8

# Picasso
-keep class com.parse.*{ *; }
-dontwarn com.parse.**
-dontwarn com.squareup.picasso.**
-keepclasseswithmembernames class * {
    native <methods>;
}