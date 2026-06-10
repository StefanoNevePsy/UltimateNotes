# kotlinx.serialization: keep serializers for the note content model.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.stefanoneve.ultimatenotes.**$$serializer { *; }
-keepclassmembers class com.stefanoneve.ultimatenotes.** {
    *** Companion;
}
-keepclasseswithmembers class com.stefanoneve.ultimatenotes.** {
    kotlinx.serialization.KSerializer serializer(...);
}
