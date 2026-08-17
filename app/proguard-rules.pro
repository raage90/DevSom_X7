# Add project specific ProGuard rules here.
#
# Minification is currently OFF (see app/build.gradle.kts) while we confirm
# the app is stable. These rules are ready for when we turn it back on --
# without rules like these, Gson/Retrofit commonly break silently (data
# comes back empty) or crash during JSON serialization, which is what
# likely caused today's crash and empty screens.

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson -- keep our data model classes and their fields so JSON parsing
# and serialization survive obfuscation/shrinking
-keep class com.galcad.app.data.** { *; }
-keep class com.google.gson.** { *; }
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers,allowobfuscation class * implements java.io.Serializable {
  <fields>;
}

# Media3 / ExoPlayer
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }
