package com.vish_apps.tasktracker

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.webkit.WebViewAssetLoader

import androidx.activity.OnBackPressedCallback

import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var cameraImageUri: Uri? = null

    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private var pendingFileChooserIntent: Intent? = null
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register activity result launchers
        fileChooserLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                // Check if result is from camera (no data or no data URI means camera)
                val resultUri = if (data?.data != null) {
                    arrayOf(data.data!!)
                } else if (cameraImageUri != null) {
                    arrayOf(cameraImageUri!!)
                } else {
                    null
                }
                filePathCallback?.onReceiveValue(resultUri)
            } else {
                filePathCallback?.onReceiveValue(null)
            }
            filePathCallback = null
        }

        cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                pendingFileChooserIntent?.let { fileChooserLauncher.launch(it) }
            } else {
                // Launch without camera option
                val galleryIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                fileChooserLauncher.launch(galleryIntent)
            }
            pendingFileChooserIntent = null
        }

        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* result ignored — if denied, native notifications are silently skipped */ }

        // Set up WebView
        webView = WebView(this)
        setContentView(webView)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                return request?.let { assetLoader.shouldInterceptRequest(it.url) }
                    ?: super.shouldInterceptRequest(view, request)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback

                // Gallery intent
                val galleryIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }

                // Camera intent
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val imageFile = createImageFile()
                cameraImageUri = FileProvider.getUriForFile(
                    this@MainActivity,
                    "${applicationContext.packageName}.fileprovider",
                    imageFile
                )
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)

                // Chooser combining both
                val chooserIntent = Intent.createChooser(galleryIntent, "Select Image").apply {
                    putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
                }

                // Request camera permission if needed
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                pendingFileChooserIntent = chooserIntent

                return true
            }
        }

        // WebView settings
        webView.settings.apply {
            javaScriptEnabled  = true   // Required: app is a JS web app
            domStorageEnabled  = true   // Required: localStorage persistence
            allowFileAccess    = false  // Safe: assets served via WebViewAssetLoader, not file://
            useWideViewPort    = true
            loadWithOverviewMode = true
            setSupportZoom(false)
            builtInZoomControls  = false
            displayZoomControls  = false
        }

        // Add JavaScript bridge for printing
        webView.addJavascriptInterface(AndroidPrintBridge(), "AndroidPrint")
        webView.addJavascriptInterface(AndroidNotifyBridge(), "AndroidNotify")

        // Handle back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Load the local web app
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        // Must be under cache/camera/ to match the restricted FileProvider path in file_paths.xml
        val cameraDir = File(cacheDir, "camera").also { it.mkdirs() }
        return File.createTempFile("IMG_${timeStamp}", ".jpg", cameraDir)
    }

    inner class AndroidPrintBridge {
        @JavascriptInterface
        fun printHtml(htmlContent: String, jobName: String) {
            runOnUiThread {
                val printWebView = WebView(this@MainActivity)
                printWebView.loadDataWithBaseURL(
                    null,
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
                printWebView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val printManager = getSystemService(PRINT_SERVICE) as PrintManager
                        val printAdapter = printWebView.createPrintDocumentAdapter(jobName)
                        val printAttributes = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .build()
                        printManager.print(jobName, printAdapter, printAttributes)
                    }
                }
            }
        }
    }

    inner class AndroidNotifyBridge {

        /**
         * Schedules (or reschedules) a reminder notification for the given task.
         *
         * Called from JavaScript as: AndroidNotify.scheduleReminder(id, title, isoTimestamp)
         *
         * IMPORTANT: @JavascriptInterface methods run on the WebView JS thread (background).
         * WorkManager.getInstance() is thread-safe so no runOnUiThread needed here.
         *
         * @param id            Task ID as a string (e.g. "42")
         * @param title         Task title to display in the notification body
         * @param isoTimestamp  ISO-8601 UTC timestamp (e.g. "2026-03-15T09:00:00.000Z")
         */
        @JavascriptInterface
        fun scheduleReminder(id: String, title: String, isoTimestamp: String) {
            val parsedMs = runCatching {
                java.time.Instant.parse(isoTimestamp).toEpochMilli()
            }.getOrNull() ?: return

            // If the timestamp is in the past, delay = 0 fires immediately.
            // This delivers reminders missed while the app was closed.
            val delayMs = (parsedMs - System.currentTimeMillis()).coerceAtLeast(0L)

            val wm = WorkManager.getInstance(applicationContext)

            // Cancel any existing job for this task (handles edits/reschedules)
            wm.cancelAllWorkByTag("reminder-$id")

            val inputData = Data.Builder()
                .putString(ReminderWorker.EXTRA_TASK_ID, id)
                .putString(ReminderWorker.EXTRA_TASK_TITLE, title)
                .build()

            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("reminder-$id")
                .build()

            wm.enqueue(request)
        }

        /**
         * Cancels any pending reminder for the given task.
         *
         * Called from JavaScript as: AndroidNotify.cancelReminder(id)
         */
        @JavascriptInterface
        fun cancelReminder(id: String) {
            WorkManager.getInstance(applicationContext).cancelAllWorkByTag("reminder-$id")
        }

        /**
         * Requests the POST_NOTIFICATIONS permission on Android 13+ (API 33+).
         * No-op on older Android versions (permission not required).
         * No-op if permission is already granted.
         * If previously denied, the OS handles it: on first-time/once-denied it shows the dialog,
         * on permanently-denied it returns DENIED immediately with no dialog shown.
         *
         * IMPORTANT: Launching an ActivityResultLauncher requires the main thread.
         * This method is called from the JS thread, so runOnUiThread is mandatory.
         */
        @JavascriptInterface
        fun requestNotificationPermission() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
            val permission = Manifest.permission.POST_NOTIFICATIONS
            // Return early only if already GRANTED — no need to ask.
            // If DENIED (never asked or asked before), call launcher.launch():
            //   - Never asked → OS shows permission dialog
            //   - Previously denied → OS shows dialog again (Android allows re-asking once)
            //   - Permanently denied → OS returns DENIED immediately, no dialog shown (no spam)
            // This is the standard Android permission-request pattern.
            if (ContextCompat.checkSelfPermission(this@MainActivity, permission)
                == PackageManager.PERMISSION_GRANTED) return
            runOnUiThread {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }
}
