# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn org.slf4j.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class net.mfuns.watch.**$$serializer { *; }
-keepclasseswithmembers class net.mfuns.watch.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Wear navigation
-keep class androidx.wear.compose.navigation.** { *; }
-dontwarn androidx.wear.compose.navigation.**

# media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
