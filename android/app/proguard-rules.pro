# Convenu ProGuard Rules

# --- Retrofit ---
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# --- OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# --- kotlinx.serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.convenu.app.data.model.**$$serializer { *; }
-keepclassmembers class com.convenu.app.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.convenu.app.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Solana Mobile ---
-keep class com.solana.mobilewalletadapter.** { *; }
-keep class com.solanamobile.** { *; }
-dontwarn com.solana.mobilewalletadapter.**
-dontwarn com.solanamobile.**

# --- Hilt ---
-dontwarn dagger.hilt.**

# --- Timber ---
-dontwarn org.jetbrains.annotations.**
