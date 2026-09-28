# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.rubberdingyrapids.baking.**$$serializer { *; }
-keepclassmembers class com.rubberdingyrapids.baking.** { *** Companion; }
-keepclasseswithmembers class com.rubberdingyrapids.baking.** { kotlinx.serialization.KSerializer serializer(...); }
