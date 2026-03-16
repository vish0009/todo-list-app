# Themes & Reminders — Design Spec

**Date:** 2026-03-13

## Goal

Add 6 switchable themes and per-task reminders with browser notifications and an in-app banner to the existing single-file HTML task tracker. Also make the app installable as a PWA (home screen shortcut).

## Themes

Six themes selectable via a dropdown/switcher in the app header. Selection persists in localStorage. Implemented via CSS custom properties scoped to `[data-theme="name"]` on `<html>`.

Themes:
- **Neon** (current default) — dark cyberpunk, glowing cyan/pink/green
- **Notebook** — cream background, ruled lines, red margin, serif font
- **Minimal** — clean white, grey borders, no decoration
- **Retro Terminal** — green on black, monospace font
- **Pastel** — soft gradient, white cards, purple accents
- **Dark Elegant** — iOS-style dark mode, blue accents

## Reminders

Each task gets a "Set Reminder" button. Clicking opens a datetime input. The chosen time is stored as `reminderAt` on the task object in localStorage.

Notification delivery:
- **In-app banner**: when the tab is open and a reminder fires, a dismissable banner appears at top of page
- **Browser notification**: OS-level notification via the Web Notifications API (requires user permission)
- **Background (PWA)**: Service Worker keeps a check loop alive so notifications fire even when the tab is in the background. Note: if the browser fully terminates the SW, background notifications may not fire — this is a browser limitation for static apps without a push server.

## PWA

- `manifest.json`: app name, icons, theme color, display mode `standalone`
- `sw.js`: service worker registered by the HTML; handles notification scheduling via message passing from the page
- "Add to Home Screen" prompt works on Android Chrome and iOS Safari

## Architecture

Single HTML file + 2 companion files:
- `TO_do_list_app.html` — all markup, inline CSS (themes), inline JS (app logic + SW registration + reminder scheduler)
- `manifest.json` — PWA manifest
- `sw.js` — service worker for background notification delivery

Data model addition to each task:
```json
{
  "reminderAt": "2026-03-14T09:00:00.000Z",
  "reminderFired": false
}
```
