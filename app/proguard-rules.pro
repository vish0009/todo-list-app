# ── WebView JavaScript Interface ──────────────────────────────────────────────
# Keep both JS bridges so they survive R8 minification in release builds
-keepclassmembers class com.vish_apps.tasktracker.MainActivity$AndroidPrintBridge {
    public *;
}
-keepclassmembers class com.vish_apps.tasktracker.MainActivity$AndroidNotifyBridge {
    public *;
}
# Keep @JavascriptInterface annotations across the codebase
-keepattributes JavascriptInterface

# ── WebView / WebKit ───────────────────────────────────────────────────────────
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void *(android.webkit.WebView, java.lang.String);
}

# ── AndroidX / Support Library ────────────────────────────────────────────────
-dontwarn androidx.**
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# ── Crash / Stack trace readability (optional but useful for Play Console) ────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin metadata ───────────────────────────────────────────────────────────
-keepattributes *Annotation*
-keepattributes Signature
