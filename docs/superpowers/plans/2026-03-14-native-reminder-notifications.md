# Native Reminder Notifications Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace broken Web Notification API reminders with native Android notifications that appear on the lock screen with sound, even when the app is fully closed.

**Architecture:** A JavaScript bridge (`AndroidNotifyBridge`) receives schedule/cancel calls from the web app and delegates to WorkManager `OneTimeWorkRequest` jobs. When a job fires, `ReminderWorker` calls `ReminderNotificationHelper` which posts a native `NotificationChannel` notification with sound and vibration. WorkManager persists jobs through app kill and device reboots.

**Tech Stack:** Kotlin, AndroidX WorkManager (`work-runtime-ktx:2.10.0`), `NotificationManager` / `NotificationCompat`, `@JavascriptInterface`, minSdk 26 (Android 8+)

---

## File Map

| Action | File | Responsibility |
|--------|------|----------------|
| Create | `app/src/main/res/drawable/ic_notification.xml` | Monochrome 24dp bell icon for notification small icon |
| Create | `app/src/main/java/com/example/myapplication/ReminderNotificationHelper.kt` | Creates `NotificationChannel` and posts the notification |
| Create | `app/src/main/java/com/example/myapplication/ReminderWorker.kt` | `CoroutineWorker` that fires when delay expires |
| Modify | `gradle/libs.versions.toml` | Add WorkManager version + library entry |
| Modify | `app/build.gradle.kts` | Add WorkManager dependency |
| Modify | `app/src/main/AndroidManifest.xml` | Add `POST_NOTIFICATIONS`, `VIBRATE`, `RECEIVE_BOOT_COMPLETED` |
| Modify | `app/src/main/java/com/example/myapplication/MainActivity.kt` | Add `AndroidNotifyBridge` inner class + permission launcher |
| Modify | `app/src/main/assets/index.html` | Wire native bridge in `saveReminder`, `clearReminder`, `toggleTask`, `deleteTask`; guard `checkReminders` |

---

## Chunk 1: Foundation — Dependency, Permissions, Drawable

### Task 1: Add WorkManager to the version catalog and build file

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Verify the Kotlin plugin is applied in app/build.gradle.kts**

  Open `app/build.gradle.kts` and check the `plugins { }` block. It should contain `alias(libs.plugins.kotlin.android)`.
  If it does **not**, add it:
  ```kotlin
  plugins {
      alias(libs.plugins.android.application)
      alias(libs.plugins.kotlin.android)   // ← add this if missing
  }
  ```
  (The `kotlin-android` alias is already defined in `gradle/libs.versions.toml`. The project may already build without it due to AGP auto-wiring, but explicit declaration is required for WorkManager's `CoroutineWorker`.)

- [ ] **Step 2: Add WorkManager version and library alias to the version catalog**

  Open `gradle/libs.versions.toml`. In the `[versions]` block, add after the last entry:
  ```toml
  workManager = "2.10.0"
  ```
  In the `[libraries]` block, add after the last entry:
  ```toml
  work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workManager" }
  ```

  > **Why 2.10.0?** WorkManager 2.10.0 (May 2024) updated ProGuard rules validated against AGP 8+/9+. Do NOT use 2.9.0 with AGP 9.x.

- [ ] **Step 3: Add the dependency to app/build.gradle.kts**

  Open `app/build.gradle.kts`. In the `dependencies { }` block, add:
  ```kotlin
  implementation(libs.work.runtime.ktx)
  ```

- [ ] **Step 4: Sync and verify the build compiles**

  In Android Studio: **File → Sync Project with Gradle Files**. Confirm the build finishes without errors. (WorkManager classes are now on the classpath.)

- [ ] **Step 5: Commit**

  ```bash
  git add gradle/libs.versions.toml app/build.gradle.kts
  git commit -m "build: add WorkManager dependency"
  ```

---

### Task 2: Add required permissions to the manifest

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add three permissions**

  Open `app/src/main/AndroidManifest.xml`. After the existing `<uses-permission android:name="android.permission.CAMERA" />` line, add:
  ```xml
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
  <uses-permission android:name="android.permission.VIBRATE" />
  <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
  ```

  > **Note on WAKE_LOCK:** WorkManager's AAR automatically merges `android.permission.WAKE_LOCK` into the merged manifest. This is expected — do NOT remove it if you see it there.

  > **Note:** Do NOT add `SCHEDULE_EXACT_ALARM` — it is for `AlarmManager.setExact()` and has no relevance here.

- [ ] **Step 2: Verify the manifest is valid**

  In Android Studio: **Build → Make Project**. Confirm no manifest merge errors.

- [ ] **Step 3: Commit**

  ```bash
  git add app/src/main/AndroidManifest.xml
  git commit -m "feat: add notification and boot permissions"
  ```

---

### Task 3: Create the monochrome notification icon

**Files:**
- Create: `app/src/main/res/drawable/ic_notification.xml`

- [ ] **Step 1: Create the file**

  Create `app/src/main/res/drawable/ic_notification.xml` with this content:
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <!-- Monochrome 24dp bell icon for system notification small icon.
       Must NOT use ic_launcher (adaptive icon) — the system requires a monochrome drawable here. -->
  <vector xmlns:android="http://schemas.android.com/apk/res/android"
      android:width="24dp"
      android:height="24dp"
      android:viewportWidth="24"
      android:viewportHeight="24"
      android:tint="@android:color/white">
      <path
          android:fillColor="@android:color/white"
          android:pathData="M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.9,2 2,2zM18,16v-5c0,-3.07 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68C7.63,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2z" />
  </vector>
  ```

- [ ] **Step 2: Verify it renders correctly**

  In Android Studio, open `ic_notification.xml` and check the preview shows a white bell on a transparent background.

- [ ] **Step 3: Commit**

  ```bash
  git add app/src/main/res/drawable/ic_notification.xml
  git commit -m "feat: add monochrome notification bell icon"
  ```

---

## Chunk 2: Notification Infrastructure

### Task 4: Create ReminderNotificationHelper

**Files:**
- Create: `app/src/main/java/com/example/myapplication/ReminderNotificationHelper.kt`

- [ ] **Step 1: Create the file**

  Create `app/src/main/java/com/example/myapplication/ReminderNotificationHelper.kt`:
  ```kotlin
  package com.example.myapplication

  import android.app.NotificationChannel
  import android.app.NotificationManager
  import android.app.PendingIntent
  import android.content.Context
  import android.content.Intent
  import android.media.AudioAttributes
  import android.media.RingtoneManager
  import androidx.core.app.NotificationCompat

  object ReminderNotificationHelper {

      private const val CHANNEL_ID = "reminder_channel"
      private const val CHANNEL_NAME = "Task Reminders"

      /**
       * Posts a heads-up notification for the given task.
       * Safe to call from any thread.
       *
       * Notification ID strategy: taskId is a numeric counter string in this app, so
       * toIntOrNull() gives zero collision risk. hashCode() is the fallback if IDs ever
       * change to non-numeric strings (acceptable for < 1000 tasks).
       */
      fun notify(context: Context, taskId: String, taskTitle: String) {
          val notificationManager =
              context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

          ensureChannel(notificationManager)

          val intent = Intent(context, MainActivity::class.java).apply {
              flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
          }
          // FLAG_IMMUTABLE required on API 31+; FLAG_UPDATE_CURRENT prevents stale cached intents
          val pendingIntent = PendingIntent.getActivity(
              context,
              taskId.toIntOrNull() ?: taskId.hashCode(),
              intent,
              PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
          )

          val notification = NotificationCompat.Builder(context, CHANNEL_ID)
              .setSmallIcon(R.drawable.ic_notification)
              .setContentTitle("Task Reminder \uD83D\uDD14")
              .setContentText(taskTitle)
              .setContentIntent(pendingIntent)
              .setAutoCancel(true)
              .build()

          val notificationId = taskId.toIntOrNull() ?: taskId.hashCode()
          notificationManager.notify(notificationId, notification)
      }

      /**
       * Creates the notification channel if it does not already exist.
       * Calling createNotificationChannel on an existing channel ID is a no-op on API 26+.
       */
      private fun ensureChannel(notificationManager: NotificationManager) {
          if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) return

          val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
          val audioAttributes = AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_NOTIFICATION)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()

          val channel = NotificationChannel(
              CHANNEL_ID,
              CHANNEL_NAME,
              NotificationManager.IMPORTANCE_HIGH   // required for lock screen heads-up display
          ).apply {
              setSound(soundUri, audioAttributes)
              enableVibration(true)
          }
          notificationManager.createNotificationChannel(channel)
      }
  }
  ```

- [ ] **Step 2: Build to confirm no compile errors**

  In Android Studio: **Build → Make Project**. Confirm it compiles cleanly.

- [ ] **Step 3: Commit**

  ```bash
  git add app/src/main/java/com/example/myapplication/ReminderNotificationHelper.kt
  git commit -m "feat: add ReminderNotificationHelper with NotificationChannel"
  ```

---

### Task 5: Create ReminderWorker

**Files:**
- Create: `app/src/main/java/com/example/myapplication/ReminderWorker.kt`

- [ ] **Step 1: Create the file**

  Create `app/src/main/java/com/example/myapplication/ReminderWorker.kt`:
  ```kotlin
  package com.example.myapplication

  import android.content.Context
  import androidx.work.CoroutineWorker
  import androidx.work.WorkerParameters

  /**
   * Fires a reminder notification for a single task.
   *
   * Scheduled as a OneTimeWorkRequest with an initialDelay equal to
   * (reminderAt - now). Tagged "reminder-<taskId>" so it can be cancelled by tag.
   *
   * WorkManager persists this job through app kill and device reboots.
   */
  class ReminderWorker(
      context: Context,
      params: WorkerParameters
  ) : CoroutineWorker(context, params) {

      override suspend fun doWork(): Result {
          val taskId = inputData.getString(EXTRA_TASK_ID) ?: return Result.failure()
          val taskTitle = inputData.getString(EXTRA_TASK_TITLE) ?: return Result.failure()

          ReminderNotificationHelper.notify(applicationContext, taskId, taskTitle)

          return Result.success()
      }

      companion object {
          const val EXTRA_TASK_ID = "task_id"
          const val EXTRA_TASK_TITLE = "task_title"
      }
  }
  ```

- [ ] **Step 2: Build to confirm no compile errors**

  In Android Studio: **Build → Make Project**. Confirm it compiles cleanly.

- [ ] **Step 3: Commit**

  ```bash
  git add app/src/main/java/com/example/myapplication/ReminderWorker.kt
  git commit -m "feat: add ReminderWorker CoroutineWorker"
  ```

---

## Chunk 3: JS Bridge — AndroidNotifyBridge + MainActivity Wiring

### Task 6: Add AndroidNotifyBridge to MainActivity and register it

**Files:**
- Modify: `app/src/main/java/com/example/myapplication/MainActivity.kt`

Context on the existing file: `MainActivity.kt` already has:
- `fileChooserLauncher` and `cameraPermissionLauncher` as `ActivityResultLauncher` fields
- An `AndroidPrintBridge` inner class registered via `webView.addJavascriptInterface(AndroidPrintBridge(), "AndroidPrint")`
- `webView.settings` configured in `onCreate`

You will follow the same pattern for the new bridge.

- [ ] **Step 1: Add the import for WorkManager, ContextCompat, and the new classes**

  At the top of `MainActivity.kt`, add these imports after the existing imports block:
  ```kotlin
  import android.content.pm.PackageManager
  import android.os.Build
  import androidx.core.content.ContextCompat
  import androidx.work.Data
  import androidx.work.OneTimeWorkRequestBuilder
  import androidx.work.WorkManager
  import java.util.concurrent.TimeUnit
  ```
  Also add this import for the POST_NOTIFICATIONS permission string check:
  ```kotlin
  import android.Manifest
  ```
  (Note: `android.Manifest` may already be imported — check before adding.)

- [ ] **Step 2: Add the notificationPermissionLauncher field**

  In `MainActivity`, after the `private var pendingFileChooserIntent: Intent? = null` field declaration, add:
  ```kotlin
  private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
  ```

- [ ] **Step 3: Register the notificationPermissionLauncher in onCreate**

  In `onCreate`, after the `cameraPermissionLauncher = registerForActivityResult(...)` block (around line 64–78), add:
  ```kotlin
  notificationPermissionLauncher = registerForActivityResult(
      ActivityResultContracts.RequestPermission()
  ) { /* result ignored — if denied, native notifications are silently skipped */ }
  ```

- [ ] **Step 4: Register the JS interface**

  In `onCreate`, after the line `webView.addJavascriptInterface(AndroidPrintBridge(), "AndroidPrint")`, add:
  ```kotlin
  webView.addJavascriptInterface(AndroidNotifyBridge(), "AndroidNotify")
  ```

- [ ] **Step 5: Add the AndroidNotifyBridge inner class**

  After the closing brace of the `AndroidPrintBridge` inner class (at the end of the file, before the final `}`), add:
  ```kotlin
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
       * No-op if permission is already granted or permanently denied.
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
  ```

- [ ] **Step 6: Build to confirm no compile errors**

  In Android Studio: **Build → Make Project**. Confirm it compiles cleanly.

- [ ] **Step 7: Smoke-test on device or emulator**

  Install the debug build. Open the app. Open browser DevTools / Logcat and verify no crash. The JS object `window.AndroidNotify` should now be available (you can verify by adding a temporary `console.log(!!window.AndroidNotify)` in the browser console — it should print `true`).

- [ ] **Step 8: Commit**

  ```bash
  git add app/src/main/java/com/example/myapplication/MainActivity.kt
  git commit -m "feat: add AndroidNotifyBridge JS interface for native reminders"
  ```

---

## Chunk 4: JS Changes — Wire the Native Bridge in index.html

### Task 7: Update saveReminder, clearReminder, toggleTask, and scheduleReminders in index.html

**Files:**
- Modify: `app/src/main/assets/index.html`

**Context on the current JS code:**

Current `saveReminder(id)` (around line 1244):
```javascript
saveReminder(id) {
    const input = document.getElementById(`reminder-input-${id}`);
    if (!input || !input.value) return;
    const task = this.tasks.find(t => t.id === id);
    if (!task) return;
    task.reminderAt = new Date(input.value).toISOString();
    task.reminderFired = false;
    this.saveTasks();
    this.scheduleReminders();
    this.render();
}
```

Current `clearReminder(id)` (around line 1256):
```javascript
clearReminder(id) {
    const task = this.tasks.find(t => t.id === id);
    if (!task) return;
    task.reminderAt = null;
    task.reminderFired = false;
    this.saveTasks();
    this.render();
}
```

Current `toggleTask(id)` task-completion block (around line 1217):
```javascript
task.completed = !task.completed;
if (task.completed) {
    task.completedAt = new Date().toISOString();
    task.duration = this.calculateDuration(task.createdAt, task.completedAt);
} else {
    task.completedAt = null;
    task.duration = null;
}
```

Current `scheduleReminders()` (around line 1302):
```javascript
scheduleReminders() {
    if (this._reminderTicker) clearInterval(this._reminderTicker);
    this._reminderTicker = setInterval(() => this.checkReminders(), 30000);
    this.notifyServiceWorker();
}
```

Current `checkReminders()` (around line 1280):
```javascript
checkReminders() {
    const now = new Date();
    let changed = false;

    this.tasks.forEach(task => {
        if (!task.reminderAt || task.reminderFired || task.completed) return;
        if (now >= new Date(task.reminderAt)) {
            task.reminderFired = true;
            changed = true;
            this.showBanner('🔔 Reminder', `"${task.title}"`);
            this.sendBrowserNotification(task.title);
        }
    });

    if (changed) {
        this.saveTasks();
        this.render();
        this.notifyServiceWorker();
    }
}
```

Current `deleteTask(id)` (around line 1230):
```javascript
deleteTask(id) {
    this.tasks = this.tasks.filter(t => t.id !== id);
    this.saveTasks();
    this.render();
}
```

- [ ] **Step 1: Update saveReminder to call the native bridge**

  Replace the body of `saveReminder(id)` so it reads:
  ```javascript
  saveReminder(id) {
      const input = document.getElementById(`reminder-input-${id}`);
      if (!input || !input.value) return;
      const task = this.tasks.find(t => t.id === id);
      if (!task) return;
      task.reminderAt = new Date(input.value).toISOString();
      task.reminderFired = false;
      this.saveTasks();
      this.scheduleReminders();   // keeps the 30s in-app banner ticker running
      if (window.AndroidNotify) {
          AndroidNotify.requestNotificationPermission();
          AndroidNotify.scheduleReminder(String(id), task.title, task.reminderAt);
      }
      this.render();
  }
  ```

- [ ] **Step 2: Update clearReminder to cancel the native job**

  Replace the body of `clearReminder(id)` so it reads:
  ```javascript
  clearReminder(id) {
      const task = this.tasks.find(t => t.id === id);
      if (!task) return;
      task.reminderAt = null;
      task.reminderFired = false;
      this.saveTasks();
      if (window.AndroidNotify) {
          AndroidNotify.cancelReminder(String(id));
      }
      this.render();
  }
  ```

- [ ] **Step 3: Update toggleTask to cancel the reminder when a task is completed**

  Find the block inside `toggleTask` that sets `task.completed = !task.completed` and the `if (task.completed)` branch. After the `if/else` block (i.e., after `task.duration = null;` and closing brace), add:
  ```javascript
  // Cancel the native reminder job when a task is marked complete
  if (task.completed && task.reminderAt && window.AndroidNotify) {
      AndroidNotify.cancelReminder(String(task.id));
  }
  ```

  The full `toggleTask` block should look like:
  ```javascript
  task.completed = !task.completed;
  if (task.completed) {
      task.completedAt = new Date().toISOString();
      task.duration = this.calculateDuration(task.createdAt, task.completedAt);
  } else {
      task.completedAt = null;
      task.duration = null;
  }
  // Cancel the native reminder job when a task is marked complete
  if (task.completed && task.reminderAt && window.AndroidNotify) {
      AndroidNotify.cancelReminder(String(task.id));
  }
  ```

- [ ] **Step 4: Update checkReminders to guard sendBrowserNotification and notifyServiceWorker**

  Replace the body of `checkReminders()` so it reads:
  ```javascript
  checkReminders() {
      const now = new Date();
      let changed = false;

      this.tasks.forEach(task => {
          if (!task.reminderAt || task.reminderFired || task.completed) return;
          if (now >= new Date(task.reminderAt)) {
              task.reminderFired = true;
              changed = true;
              this.showBanner('🔔 Reminder', `"${task.title}"`);
              // Browser fallback only — native bridge handles this on Android
              if (!window.AndroidNotify) {
                  this.sendBrowserNotification(task.title);
              }
          }
      });

      if (changed) {
          this.saveTasks();
          this.render();
          // Browser fallback only — native bridge handles this on Android
          if (!window.AndroidNotify) {
              this.notifyServiceWorker();
          }
      }
  }
  ```

- [ ] **Step 5: Update scheduleReminders to skip notifyServiceWorker when native bridge is present**

  Replace the body of `scheduleReminders()` so it reads:
  ```javascript
  scheduleReminders() {
      if (this._reminderTicker) clearInterval(this._reminderTicker);
      this._reminderTicker = setInterval(() => this.checkReminders(), 30000);
      // Only send to Service Worker if native Android bridge is unavailable (browser fallback)
      if (!window.AndroidNotify) {
          this.notifyServiceWorker();
      }
  }
  ```

- [ ] **Step 6: Update deleteTask to cancel the native reminder job**

  Replace the body of `deleteTask(id)` so it reads:
  ```javascript
  deleteTask(id) {
      const task = this.tasks.find(t => t.id === id);
      if (task && task.reminderAt && window.AndroidNotify) {
          AndroidNotify.cancelReminder(String(id));
      }
      this.tasks = this.tasks.filter(t => t.id !== id);
      this.saveTasks();
      this.render();
  }
  ```

- [ ] **Step 7: Build and install on device/emulator**

  In Android Studio: **Run → Run 'app'** (or use `./gradlew installDebug`).

- [ ] **Step 8: Manual smoke test — schedule a near-future reminder**

  1. Open the app, add a task.
  2. Set a reminder 1 minute in the future.
  3. Kill the app (swipe away from recents).
  4. Wait ~1 minute.
  5. Verify: a notification with sound appears on the lock screen. Tapping it opens the app.

- [ ] **Step 9: Commit**

  ```bash
  git add app/src/main/assets/index.html
  git commit -m "feat: wire native AndroidNotify bridge in JS reminder handlers"
  ```

---

## Final Manual Testing Checklist

Run all of these on a physical device (emulators have limited notification sound support):

- [ ] Set a reminder 1–2 minutes in the future; close the app; verify notification appears on lock screen **with sound**
- [ ] Set a reminder; mark the task complete; verify **no** notification fires
- [ ] Set a reminder; tap Clear; verify **no** notification fires
- [ ] Set a reminder; change it to a new time; verify notification fires at the **new** time (not old)
- [ ] Set two reminders at nearly the same time; verify **both** notifications appear (no collision / one overwriting the other)
- [ ] On Android 13+ device: verify permission dialog appears on **first** reminder save
- [ ] On Android 13+: **deny** the permission dialog; set a reminder; verify app does not crash; in-app banner still shows when app is open
- [ ] On Android < 13: verify no permission crash
- [ ] Tap a notification; verify the app **opens / focuses**
- [ ] Kill app; advance device clock past a reminder time; relaunch app; verify notification fires **immediately**
- [ ] Enable Battery Optimization for the app (`Settings → Apps → [App] → Battery → Restricted`); set a reminder 2 minutes out; lock device; verify notification eventually fires (up to ~15 min latency is acceptable)
