# ───────────────────────────────────────────────────────────────
# Orbit — R8 / ProGuard keep rules
# Retrofit, OkHttp, Hilt and kotlinx.serialization ship their own
# consumer rules; only precise serializer keeps are added here.
# ───────────────────────────────────────────────────────────────

-keepattributes RuntimeVisibleAnnotations, AnnotationDefault, Signature, InnerClasses, EnclosingMethod

# kotlinx.serialization — keep generated serializer hooks (precise)
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static <1>$Companion Companion;
}
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Suppress missing-class noise from optional transitive deps
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
