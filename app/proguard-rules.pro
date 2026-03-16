# ── WebView JavaScript Interface ──────────────────────────────────────────────
# Keep the AndroidPrintBridge so the JS bridge works after R8 minification
-keepclassmembers class com.example.myapplication.MainActivity$AndroidPrintBridge {
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
