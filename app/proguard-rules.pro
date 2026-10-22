-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn org.slf4j.**

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class net.mfuns.watch.**$$serializer { *; }
-keepclasseswithmembers class net.mfuns.watch.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class androidx.wear.compose.navigation.** { *; }
-dontwarn androidx.wear.compose.navigation.**

-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
