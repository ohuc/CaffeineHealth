# Keep Room entities, DAOs, and database classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.TypeConverter class * { *; }

# Keep Kotlin data classes used by Room (prevents column-mapping breakage)
-keepclassmembers class com.uc.caffeine.data.model.** {
    public <init>(...);
    public ** component*();
    public ** copy(...);
    public ** get*();
    public ** set*();
    *;
}

# Keep Kotlin metadata for reflection used by coroutines / Room
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# Enum classes — prevent name mangling
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Vico charts
-keep class com.patrykandpatrick.vico.** { *; }
-dontwarn com.patrykandpatrick.vico.**

# Coil image loading
-keep class io.coil3.** { *; }
-dontwarn io.coil3.**

# DataStore Preferences
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite* {
   <fields>;
}

# Preserve source file names and line numbers for readable crash stack traces
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# Disable name obfuscation so bytecode is identical across JVM versions (required for reproducible builds)
-dontobfuscate