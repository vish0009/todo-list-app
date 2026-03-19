# Privacy Policy — Task Tracker

**Effective date:** 2026-03-19
**Developer:** Vish (com.vish_apps.tasktracker)
**Contact:** vishapps.support@gmail.com

---

## 1. Overview

Task Tracker is a personal productivity app that helps you manage tasks, set reminders, and track priorities. This policy explains what data the app accesses, how it is used, and your rights as a user.

**Short version:** Everything you enter stays on your device. We do not collect, transmit, or share any of your personal data.

---

## 2. Data We Collect

**We do not collect any personal data.**

All task data (titles, priorities, reminders, completion status) is stored exclusively in your device's local storage (`localStorage`). This data never leaves your device and is never transmitted to any server, cloud service, or third party.

---

## 3. Permissions Used

### Camera (`android.permission.CAMERA`)
- **Purpose:** Allows you to take a photo directly from the app to attach to a task.
- **What we do with it:** The photo is saved to your device's temporary cache folder and displayed within the app. It is never uploaded, transmitted, or shared.
- **Optional:** This permission is entirely optional. If denied, the app falls back to your device's photo gallery. All app features except camera capture remain fully available.

### Notifications (`android.permission.POST_NOTIFICATIONS`)
- **Purpose:** Sends you a local reminder notification at the date and time you set for a task.
- **What we do with it:** Notifications are generated entirely on-device by Android's WorkManager. No notification content is transmitted externally.
- **Optional:** If denied, in-app reminder banners still appear when the app is open.

### Vibration (`android.permission.VIBRATE`)
- **Purpose:** Makes the device vibrate when a reminder notification is delivered.
- **Data collected:** None.

### Receive Boot Completed (`android.permission.RECEIVE_BOOT_COMPLETED`)
- **Purpose:** Restores scheduled reminders after the device restarts, so you don't miss reminders set before a reboot.
- **Data collected:** None. No data is read from or written to any external service.

---

## 4. Data Storage

| Data type | Where stored | Leaves device? |
|-----------|-------------|----------------|
| Task titles, priorities, dates | Device localStorage | ❌ Never |
| Reminder times | Device localStorage | ❌ Never |
| Camera photos (temp) | Device cache (`/cache/camera/`) | ❌ Never |
| Theme preference | Device localStorage | ❌ Never |

---

## 5. Third-Party Services

Task Tracker does not integrate any third-party analytics, advertising, crash reporting, or tracking SDKs. There are no third-party services that receive any data from this app.

---

## 6. Children's Privacy

Task Tracker does not collect personal information from anyone, including children under the age of 13. The app contains no advertising, no account creation, and no data transmission of any kind.

---

## 7. Data Deletion

Since all data is stored locally on your device, you can delete it at any time by:
- Deleting individual tasks using the Delete button inside the app, or
- Clearing the app's data via **Android Settings → Apps → Task Tracker → Storage → Clear Data**, or
- Uninstalling the app, which removes all associated data from your device.

---

## 8. Security

All data remains on your device and is subject to Android's built-in application sandboxing. We do not transmit data over any network, so there is no risk of network interception.

---

## 9. Changes to This Policy

If we update this policy, the new version will be posted at this same URL with an updated effective date. Continued use of the app after changes constitutes acceptance of the updated policy.

---

## 10. Contact

If you have any questions about this privacy policy, please contact:

**Email:** vishapps.support@gmail.com

---

*This privacy policy was last updated on 2026-03-19.*
