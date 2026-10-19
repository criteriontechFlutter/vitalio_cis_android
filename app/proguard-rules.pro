# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# ---------------------------------------------------------------------------
# Vitalio rules
# ---------------------------------------------------------------------------

# Readable crash stack traces in Crashlytics (upload mapping.txt is automatic).
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*
-renamesourcefileattribute SourceFile

# Gson models are spread across many packages (model/, domain/model/, reminder/,
# viewmodel/, ring/ ...) and are parsed by reflection, so field names must survive.
# Keep all app classes for now; narrow this to the model classes once they are
# consolidated, to also get obfuscation of app code.
-keep class com.critetiontech.ctvitalio.** { *; }
-keep class com.example.vitalio_cis.** { *; }
-keep class com.critetiontech.myapplication.** { *; }

# Gson generic type capture (TypeToken<List<...>>)
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Strip debug/verbose/info logs and println from release builds so no
# patient data (API bodies, tokens) ends up in logcat.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
-assumenosideeffects class kotlin.io.ConsoleKt {
    public static void println(...);
}
