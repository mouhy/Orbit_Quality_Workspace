# Keep serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.orbit.mobile.data.dto.** {
    *** Companion;
}
