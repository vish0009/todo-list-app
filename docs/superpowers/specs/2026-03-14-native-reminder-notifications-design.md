# Native Reminder Notifications — Design Spec
**Date:** 2026-03-14
**Status:** Approved

## Problem

The app currently delivers reminders via two mechanisms that both fail on Android:

1. **In-app banners** — only visible when the app is open and focused.
2. **Web Notification API + Service Worker** — not supported inside Android WebViews. No sound, no lock screen appearance.

Users receive no notification when the app is in the background or closed.

## Goal

Reminders must appear on the lock screen and home screen with notification sound, even when the app is fully closed (killed from recents).

## Chosen Approach

**WorkManager `OneTimeWorkRequest` with `initialDelay` + native `NotificationChannel`.**

Each reminder is persisted as a WorkManager job. When the delay expires, a `CoroutineWorker` fires a native Android notification (sound + vibration + lock screen). WorkManager survives app kill and device reboots via its own internal boot receiver (combined with `RECEIVE_BOOT_COMPLETED` permission declared in the manifest).

**Trade-off accepted:** WorkManager may fire a few seconds to minutes late in Android Doze mode. This is acceptable for reminder use cases.

## Architecture

```
Web App (JS)
    │  AndroidNotify.scheduleReminder(id, title, isoTimestamp)
    │  AndroidNotify.cancelReminder(id)
    │  AndroidNotify.requestNotificationPermission()
    ▼
AndroidNotifyBridge  [JavascriptInterface — MainActivity.kt inner class]
    │  scheduleReminder → cancel existing tag + enqueue OneTimeWorkRequest
    │  cancelReminder   → cancel by tag "reminder-$id"
    │  requestNotificationPermission → runOnUiThread { permissionLauncher.launch() }
    ▼
ReminderWorker  [CoroutineWorker]
    │  receives taskId + taskTitle via inputData
    │  calls ReminderNotificationHelper.notify()
    ▼
ReminderNotificationHelper
    └─ NotificationChannel (IMPORTANCE_HIGH, sound + AudioAttributes, vibration)
       NotificationCompat.Builder (contentIntent, unique notification ID)
       → NotificationManager.notify()
```

## New Files

| File | Purpose |
|------|---------|
| `app/src/main/java/com/example/myapplication/ReminderWorker.kt` | CoroutineWorker that fires the notification |
| `app/src/main/java/com/example/myapplication/ReminderNotificationHelper.kt` | Creates NotificationChannel and builds/posts the notification |
| `app/src/main/res/drawable/ic_notification.xml` | Monochrome (white-on-transparent) 24×24dp vector drawable for the notification small icon |

## Modified Files

| File | Change |
|------|--------|
| `MainActivity.kt` | Add `AndroidNotifyBridge` inner class; register JS interface; handle `POST_NOTIFICATIONS` permission via `ActivityResultLauncher` |
| `AndroidManifest.xml` | Add `POST_NOTIFICATIONS`, `VIBRATE`, `RECEIVE_BOOT_COMPLETED` permissions |
| `app/build.gradle.kts` | Add `androidx.work:work-runtime-ktx` dependency |
| `app/src/main/assets/index.html` | Call native bridge in `saveReminder()`, `clearReminder()`, and task completion; request permission on first reminder save; keep in-app banner; add browser fallback guard |

## Component Details

### ic_notification.xml

A 24×24dp monochrome vector drawable (white fill on transparent background), placed in `res/drawable/`. This is required for the notification small icon — the launcher icon (`ic_launcher`) must NOT be used as it is an adaptive icon and is not supported as a notification small icon.

### ReminderNotificationHelper.kt

- Channel ID: `reminder_channel`
- Channel name: `"Task Reminders"`
- Importance: `NotificationManager.IMPORTANCE_HIGH` (required for lock screen heads-up display)
- Sound: `RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)` set via `channel.setSound(soundUri, audioAttributes)`
- AudioAttributes: `AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()`
- Vibration: enabled on channel
- Notification small icon: `R.drawable.ic_notification` (monochrome vector)
- Content intent: `PendingIntent` launching `MainActivity` with `PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT` (required on Android 12+; `FLAG_UPDATE_CURRENT` ensures any future extras are not stale)
- Auto-cancel: true
- Notification ID: `taskId.toIntOrNull() ?: taskId.hashCode()` — if taskId is a simple numeric counter (as it is in this app), cast directly to Int for zero collision risk. If IDs ever change to non-numeric strings, `hashCode()` is an accepted fallback with low collision probability across typical task counts (< 1000). Document this assumption in code comments.

### ReminderWorker.kt

- Input data keys: `EXTRA_TASK_ID` (String), `EXTRA_TASK_TITLE` (String)
- `doWork()`: create notification channel (idempotent), post notification using `ReminderNotificationHelper`, return `Result.success()`
- Tag format: `"reminder-<taskId>"` — used for cancellation

### AndroidNotifyBridge (inner class in MainActivity)

```kotlin
@JavascriptInterface
fun scheduleReminder(id: String, title: String, isoTimestamp: String)
// 1. Parse isoTimestamp → delay ms
// 2. If delay <= 0, set delay = 0 (fire immediately — delivers missed reminders on relaunch)
// 3. WorkManager.getInstance(applicationContext).cancelAllWorkByTag("reminder-$id")
// 4. Enqueue OneTimeWorkRequest with initialDelay + inputData(id, title) + tag "reminder-$id"
// NOTE: use applicationContext (not Activity context) to avoid leaks and survive rotation

@JavascriptInterface
fun cancelReminder(id: String)
// WorkManager.getInstance(applicationContext).cancelAllWorkByTag("reminder-$id")

@JavascriptInterface
fun requestNotificationPermission()
// On Android 13+ (API 33):
//   Check ContextCompat.checkSelfPermission(POST_NOTIFICATIONS) first.
//   If already GRANTED or DENIED, do NOT re-launch the dialog.
//   Only launch if permission is still in the DEFAULT (not-yet-asked) state.
//   runOnUiThread { notificationPermissionLauncher.launch(POST_NOTIFICATIONS) }
// On older Android: no-op (permission not required)
// NOTE: JavascriptInterface methods run on a background thread — UI calls MUST use runOnUiThread
```

**Thread safety:** All `@JavascriptInterface` methods are called on the WebView's JavaScript thread (a background thread). Any UI operations (launching permission dialogs, accessing UI components) must be wrapped in `runOnUiThread { }`.

### index.html changes

```javascript
// In saveReminder(id) — after setting task.reminderAt:
if (window.AndroidNotify) {
    AndroidNotify.requestNotificationPermission(); // no-op if already granted or Android < 13
    AndroidNotify.scheduleReminder(String(id), task.title, task.reminderAt);
} else {
    this.notifyServiceWorker(); // browser fallback
}

// In clearReminder(id):
if (window.AndroidNotify) {
    AndroidNotify.cancelReminder(String(id));
}

// When a task is marked complete (wherever completedAt is set):
if (window.AndroidNotify) {
    AndroidNotify.cancelReminder(String(task.id));
}
```

- `sendBrowserNotification()` and `notifyServiceWorker()` are kept but only called when `window.AndroidNotify` is absent (browser fallback).
- `scheduleReminders()` polling ticker kept for in-app banner display only.

## Permissions (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

Note: `SCHEDULE_EXACT_ALARM` is NOT needed — WorkManager does not use `AlarmManager.setExact()`.

## Dependencies (app/build.gradle.kts)

```kotlin
implementation("androidx.work:work-runtime-ktx:2.9.0")
```

**Note on `WAKE_LOCK`:** The WorkManager AAR automatically merges `android.permission.WAKE_LOCK` into the app's merged manifest. This is expected and intentional — WorkManager acquires a wake lock internally to ensure `doWork()` runs to completion even when the device is asleep. No action needed; do not remove it if seen in the merged manifest.

## Testing Checklist

- [ ] Set a reminder 1–2 minutes in the future; close the app; verify notification appears on lock screen with sound
- [ ] Set a reminder; mark the task complete; verify no notification fires
- [ ] Set a reminder; clear it; verify no notification fires
- [ ] Set a reminder; edit it to a new time; verify notification fires at new time (not old)
- [ ] Set two reminders at nearly the same time; verify both notifications appear (no collision)
- [ ] On Android 13+ device: verify permission dialog appears on first reminder save
- [ ] On Android 13+: deny the permission dialog; set a reminder; verify app does not crash and degrades gracefully (in-app banner still shows when app is open)
- [ ] On Android < 13: verify no permission crash
- [ ] Tap the notification; verify the app opens / focuses
- [ ] Set a reminder for a time that has already passed (simulate by killing app, advancing clock past reminder time, relaunching); verify notification fires immediately on relaunch
- [ ] Enable Battery Optimization for the app; set a reminder 2 minutes out; lock the device; verify notification eventually fires (latency up to ~15 min acceptable, but must fire)
