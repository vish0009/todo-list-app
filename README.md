# Task Tracker

A lightweight to-do list app built as a hybrid Android app (native WebView wrapping a single-file web app).

## Features
- Add, complete and delete tasks
- 5-star priority rating per task
- Reminders with OS notifications and in-app banners
- 6 switchable themes: Neon, Notebook, Minimal, Retro Terminal, Pastel, Dark Elegant
- Sort and filter tasks
- Task duration tracking
- Offline support (Service Worker / PWA)
- Print tasks via Android print framework

## Tech Stack
- **Android**: Kotlin, WebView, WebViewAssetLoader, CameraX file chooser
- **Web layer**: Vanilla HTML/CSS/JS, localStorage, Web Notifications API, Service Worker

## Build

```bash
./gradlew assembleDebug     # debug APK
./gradlew assembleRelease   # release APK (requires keystore — see below)
```

## Signing Setup

Release signing credentials are **never** stored in source control.
Add the following to your `local.properties` (already gitignored):

```properties
RELEASE_STORE_FILE=../keystore.jks
RELEASE_STORE_PASSWORD=your_password
RELEASE_KEY_ALIAS=release
RELEASE_KEY_PASSWORD=your_password
```

## Version History

| Version | Code | Notes |
|---------|------|-------|
| 1.0     | 1    | Initial release |

## Requirements
- Android 8.0+ (API 26)
- Camera permission optional — app falls back to gallery if denied
