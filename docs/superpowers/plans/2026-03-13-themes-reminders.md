# Themes & Reminders Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 6 switchable themes, per-task reminders with OS/in-app notifications, and PWA home screen installability to the existing single-file HTML task tracker.

**Architecture:** CSS custom properties handle all theming via `[data-theme]` on `<html>`. Reminders are stored on task objects in localStorage and checked every 30 seconds by a ticker in the page JS; a Service Worker receives reminder schedules via postMessage to fire OS notifications in the background. Two new companion files (`manifest.json`, `sw.js`) are added alongside the existing HTML.

**Tech Stack:** Vanilla HTML/CSS/JS, Web Notifications API, Service Worker API, PWA manifest, localStorage.

---

## Chunk 1: Theme System

### Task 1: Add CSS theme definitions

**Files:**
- Modify: `TO_do_list_app.html` — replace `:root` block and add per-theme variable sets

- [ ] **Step 1: Open `TO_do_list_app.html` and locate the `:root` CSS block (lines 8–36)**

- [ ] **Step 2: Replace the entire `:root` block and the `@media (prefers-color-scheme: dark)` block with the following theme definitions**

Replace this:
```css
        :root {
            --color-bg-primary: #0a0e27;
            ... (entire :root and @media block, lines 8-36)
        }
```

With this (paste after the opening `<style>` tag, before the `* { margin: 0; }` rule):

```css
        /* ── NEON (default) ── */
        :root, [data-theme="neon"] {
            --color-bg-primary: #0a0e27;
            --color-surface: #1a1f3a;
            --color-text-primary: #00ff9f;
            --color-text-secondary: #00d4ff;
            --color-primary: #ff00ff;
            --color-primary-hover: #cc00cc;
            --color-border: rgba(0, 255, 159, 0.3);
            --color-success: #00ff9f;
            --color-error: #ff0055;
            --color-neon-pink: #ff00ff;
            --color-neon-cyan: #00d4ff;
            --color-neon-green: #00ff9f;
            --color-neon-yellow: #ffff00;
            --body-font: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            --body-text-shadow: 0 0 10px rgba(0, 255, 159, 0.5);
            --task-border: 1px solid var(--color-neon-cyan);
            --task-shadow: 0 0 10px rgba(0, 212, 255, 0.2);
            --task-hover-shadow: 0 0 25px rgba(0, 212, 255, 0.6), 0 0 40px rgba(255, 0, 255, 0.3);
            --form-border: 2px solid var(--color-neon-pink);
            --form-shadow: 0 0 20px rgba(255, 0, 255, 0.4), inset 0 0 20px rgba(255, 0, 255, 0.1);
            --stat-border: 2px solid var(--color-neon-green);
            --stat-shadow: 0 0 15px rgba(0, 255, 159, 0.3);
            --border-radius-task: 6px;
            --border-radius-form: 8px;
        }

        /* ── NOTEBOOK ── */
        [data-theme="notebook"] {
            --color-bg-primary: #fdf6e3;
            --color-surface: transparent;
            --color-text-primary: #3a3028;
            --color-text-secondary: #8a6a3a;
            --color-primary: #c0392b;
            --color-primary-hover: #96281b;
            --color-border: #c8a87a;
            --color-success: #27ae60;
            --color-error: #c0392b;
            --color-neon-pink: #c0392b;
            --color-neon-cyan: #c8a87a;
            --color-neon-green: #27ae60;
            --color-neon-yellow: #f39c12;
            --body-font: 'Georgia', 'Times New Roman', serif;
            --body-text-shadow: none;
            --task-border: none;
            --task-shadow: none;
            --task-hover-shadow: 0 2px 8px rgba(0,0,0,0.1);
            --form-border: 1px solid #c8a87a;
            --form-shadow: none;
            --stat-border: 1px solid #c8a87a;
            --stat-shadow: none;
            --border-radius-task: 0;
            --border-radius-form: 4px;
        }

        /* ── MINIMAL ── */
        [data-theme="minimal"] {
            --color-bg-primary: #f9fafb;
            --color-surface: #ffffff;
            --color-text-primary: #111827;
            --color-text-secondary: #6b7280;
            --color-primary: #111827;
            --color-primary-hover: #374151;
            --color-border: #e5e7eb;
            --color-success: #059669;
            --color-error: #dc2626;
            --color-neon-pink: #e5e7eb;
            --color-neon-cyan: #e5e7eb;
            --color-neon-green: #059669;
            --color-neon-yellow: #d97706;
            --body-font: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            --body-text-shadow: none;
            --task-border: 1px solid var(--color-border);
            --task-shadow: none;
            --task-hover-shadow: 0 1px 4px rgba(0,0,0,0.08);
            --form-border: 1px solid var(--color-border);
            --form-shadow: none;
            --stat-border: 1px solid var(--color-border);
            --stat-shadow: none;
            --border-radius-task: 6px;
            --border-radius-form: 8px;
        }

        /* ── RETRO TERMINAL ── */
        [data-theme="retro"] {
            --color-bg-primary: #0d0d0d;
            --color-surface: #111111;
            --color-text-primary: #33ff33;
            --color-text-secondary: #1a9a1a;
            --color-primary: #33ff33;
            --color-primary-hover: #22cc22;
            --color-border: #1a3a1a;
            --color-success: #33ff33;
            --color-error: #ff3333;
            --color-neon-pink: #33ff33;
            --color-neon-cyan: #1a9a1a;
            --color-neon-green: #33ff33;
            --color-neon-yellow: #ffff00;
            --body-font: 'Courier New', 'Lucida Console', monospace;
            --body-text-shadow: 0 0 8px rgba(51, 255, 51, 0.4);
            --task-border: 1px solid #1a3a1a;
            --task-shadow: none;
            --task-hover-shadow: 0 0 10px rgba(51, 255, 51, 0.3);
            --form-border: 1px solid #33ff33;
            --form-shadow: 0 0 10px rgba(51, 255, 51, 0.2);
            --stat-border: 1px solid #1a3a1a;
            --stat-shadow: none;
            --border-radius-task: 0;
            --border-radius-form: 0;
        }

        /* ── PASTEL ── */
        [data-theme="pastel"] {
            --color-bg-primary: #f0e6ff;
            --color-surface: #ffffff;
            --color-text-primary: #3d3d5c;
            --color-text-secondary: #9e86c8;
            --color-primary: #9e86c8;
            --color-primary-hover: #7d65a8;
            --color-border: #ddd0f0;
            --color-success: #a8d5a2;
            --color-error: #f4a0a0;
            --color-neon-pink: #f4a0c8;
            --color-neon-cyan: #a0d4f4;
            --color-neon-green: #a8d5a2;
            --color-neon-yellow: #f4e4a0;
            --body-font: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            --body-text-shadow: none;
            --task-border: none;
            --task-shadow: 0 2px 8px rgba(158, 134, 200, 0.15);
            --task-hover-shadow: 0 4px 16px rgba(158, 134, 200, 0.3);
            --form-border: 1px solid #ddd0f0;
            --form-shadow: 0 2px 12px rgba(158, 134, 200, 0.2);
            --stat-border: none;
            --stat-shadow: 0 2px 8px rgba(158, 134, 200, 0.15);
            --border-radius-task: 12px;
            --border-radius-form: 16px;
        }

        /* ── DARK ELEGANT ── */
        [data-theme="dark-elegant"] {
            --color-bg-primary: #1c1c1e;
            --color-surface: #2c2c2e;
            --color-text-primary: #f5f5f7;
            --color-text-secondary: #8e8e93;
            --color-primary: #0a84ff;
            --color-primary-hover: #0070d8;
            --color-border: #3a3a3c;
            --color-success: #30d158;
            --color-error: #ff453a;
            --color-neon-pink: #bf5af2;
            --color-neon-cyan: #0a84ff;
            --color-neon-green: #30d158;
            --color-neon-yellow: #ffd60a;
            --body-font: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
            --body-text-shadow: none;
            --task-border: none;
            --task-shadow: none;
            --task-hover-shadow: 0 2px 12px rgba(0,0,0,0.4);
            --form-border: none;
            --form-shadow: none;
            --stat-border: none;
            --stat-shadow: none;
            --border-radius-task: 10px;
            --border-radius-form: 12px;
        }
```

- [ ] **Step 3: Update existing CSS rules to use the new variables**

In the CSS, find these rules and update them:

```css
        body {
            font-family: var(--body-font);
            background: var(--color-bg-primary);
            color: var(--color-text-primary);
            padding: 20px;
            line-height: 1.6;
            text-shadow: var(--body-text-shadow);
        }

        .add-task-form {
            background: var(--color-surface);
            padding: 1.5rem;
            border-radius: var(--border-radius-form);
            border: var(--form-border);
            margin-bottom: 2rem;
            box-shadow: var(--form-shadow);
        }

        .task-item {
            background: var(--color-surface);
            padding: 0.875rem 1rem;
            border-radius: var(--border-radius-task);
            border: var(--task-border);
            display: flex;
            align-items: center;
            gap: 0.75rem;
            transition: all 0.3s;
            box-shadow: var(--task-shadow);
        }

        .task-item:hover {
            box-shadow: var(--task-hover-shadow);
            transform: translateY(-2px);
        }

        .stat-card {
            background: var(--color-surface);
            padding: 1rem;
            border-radius: var(--border-radius-form);
            border: var(--stat-border);
            flex: 1;
            min-width: 150px;
            box-shadow: var(--stat-shadow);
        }
```

Also add a special notebook body background (after all the theme variable blocks, before `* {}`):

```css
        /* Notebook ruled lines */
        [data-theme="notebook"] body {
            background-image: repeating-linear-gradient(
                transparent, transparent 31px, #b8d4e8 31px, #b8d4e8 32px
            );
            background-attachment: local;
        }

        [data-theme="notebook"] .container {
            border-left: 3px solid #e88;
            padding-left: 2rem;
        }

        [data-theme="retro"] h1::before {
            content: 'C:\\TASKS> ';
            font-size: 0.7em;
            opacity: 0.6;
        }
```

- [ ] **Step 4: Verify visually** — open the HTML file in a browser, confirm it still looks like the neon theme (no regressions).

---

### Task 2: Add theme switcher UI + JS

**Files:**
- Modify: `TO_do_list_app.html` — add switcher to header, add theme CSS, add theme JS

- [ ] **Step 1: Add theme switcher CSS** (paste into `<style>` block before `</style>`):

```css
        /* ── Theme Switcher ── */
        .header-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 2rem;
            flex-wrap: wrap;
            gap: 1rem;
        }

        .header-row h1 {
            margin-bottom: 0;
        }

        .theme-switcher {
            display: flex;
            align-items: center;
            gap: 0.5rem;
        }

        .theme-label {
            font-size: 0.8rem;
            color: var(--color-text-secondary);
            white-space: nowrap;
        }

        .theme-select {
            padding: 0.4rem 2rem 0.4rem 0.6rem;
            border: 1px solid var(--color-border);
            border-radius: 6px;
            background: var(--color-surface);
            color: var(--color-text-primary);
            cursor: pointer;
            font-size: 0.8rem;
            font-family: var(--body-font);
            appearance: none;
            background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23888' d='M6 9L1 4h10z'/%3E%3C/svg%3E");
            background-repeat: no-repeat;
            background-position: right 0.5rem center;
        }
```

- [ ] **Step 2: Update the HTML header** — find `<h1>📝 Task Tracker</h1>` and wrap it:

Replace:
```html
        <h1>📝 Task Tracker</h1>
```

With:
```html
        <div class="header-row">
            <h1>📝 Task Tracker</h1>
            <div class="theme-switcher">
                <span class="theme-label">Theme:</span>
                <select id="themeSelect" class="theme-select">
                    <option value="neon">Neon</option>
                    <option value="notebook">Notebook</option>
                    <option value="minimal">Minimal</option>
                    <option value="retro">Retro Terminal</option>
                    <option value="pastel">Pastel</option>
                    <option value="dark-elegant">Dark Elegant</option>
                </select>
            </div>
        </div>
```

- [ ] **Step 3: Add theme JS** — in the `<script>` block, inside the `TaskTracker` constructor's `init()` method, add after `this.initPriorityRating();`:

First add a property in the constructor (after `this.currentSort = 'created-desc';`):
```js
                this.currentTheme = localStorage.getItem('theme') || 'neon';
```

Then add these two methods to the `TaskTracker` class (before the closing `}`):

```js
            initTheme() {
                const select = document.getElementById('themeSelect');
                select.value = this.currentTheme;
                document.documentElement.setAttribute('data-theme', this.currentTheme);
                select.addEventListener('change', (e) => {
                    this.currentTheme = e.target.value;
                    localStorage.setItem('theme', this.currentTheme);
                    document.documentElement.setAttribute('data-theme', this.currentTheme);
                });
            }
```

And call it from `init()` — add `this.initTheme();` after `this.initPriorityRating();`.

- [ ] **Step 4: Open the app in a browser and test theme switching** — switch through all 6 themes, reload the page, confirm the last selected theme is restored.

- [ ] **Step 5: Commit**

```bash
git add "TO_do_list_app.html"
git commit -m "feat: add 6 switchable themes (neon, notebook, minimal, retro, pastel, dark-elegant)"
```

---

## Chunk 2: Reminder System (In-App)

### Task 3: Extend task data model + reminder UI

**Files:**
- Modify: `TO_do_list_app.html` — add reminder fields to task object, add reminder UI in task cards

- [ ] **Step 1: Add reminder CSS** (add to `<style>` block):

```css
        /* ── Reminders ── */
        .btn-reminder {
            padding: 0.4rem 0.75rem;
            border: 1px solid var(--color-border);
            border-radius: 4px;
            background: transparent;
            color: var(--color-text-secondary);
            cursor: pointer;
            font-size: 0.8rem;
            transition: all 0.2s;
            white-space: nowrap;
        }

        .btn-reminder:hover {
            border-color: var(--color-primary);
            color: var(--color-primary);
        }

        .btn-reminder.has-reminder {
            color: var(--color-success);
            border-color: var(--color-success);
        }

        .reminder-picker {
            display: none;
            margin-top: 0.5rem;
            padding: 0.5rem;
            background: var(--color-bg-primary);
            border: 1px solid var(--color-border);
            border-radius: 6px;
            gap: 0.5rem;
            align-items: center;
            flex-wrap: wrap;
        }

        .reminder-picker.open {
            display: flex;
        }

        .reminder-input {
            padding: 0.4rem 0.6rem;
            border: 1px solid var(--color-border);
            border-radius: 4px;
            background: var(--color-surface);
            color: var(--color-text-primary);
            font-size: 0.8rem;
            font-family: var(--body-font);
        }

        .reminder-input:focus {
            outline: 2px solid var(--color-primary);
        }

        .btn-reminder-save {
            padding: 0.4rem 0.75rem;
            border: none;
            border-radius: 4px;
            background: var(--color-primary);
            color: white;
            cursor: pointer;
            font-size: 0.8rem;
            transition: background 0.2s;
        }

        .btn-reminder-save:hover {
            background: var(--color-primary-hover);
        }

        .btn-reminder-clear {
            padding: 0.4rem 0.75rem;
            border: 1px solid var(--color-border);
            border-radius: 4px;
            background: transparent;
            color: var(--color-error);
            cursor: pointer;
            font-size: 0.8rem;
        }

        /* ── In-App Notification Banner ── */
        .notification-banner {
            position: fixed;
            top: 20px;
            right: 20px;
            max-width: 360px;
            background: var(--color-surface);
            border: 2px solid var(--color-primary);
            border-radius: 10px;
            padding: 1rem 1.25rem;
            box-shadow: 0 4px 24px rgba(0,0,0,0.3);
            z-index: 9999;
            display: flex;
            flex-direction: column;
            gap: 0.5rem;
            animation: slideIn 0.3s ease;
        }

        @keyframes slideIn {
            from { transform: translateX(120%); opacity: 0; }
            to   { transform: translateX(0);    opacity: 1; }
        }

        .notification-banner-title {
            font-weight: 600;
            font-size: 0.95rem;
            color: var(--color-primary);
        }

        .notification-banner-body {
            font-size: 0.875rem;
            color: var(--color-text-primary);
        }

        .notification-banner-close {
            align-self: flex-end;
            background: none;
            border: none;
            color: var(--color-text-secondary);
            cursor: pointer;
            font-size: 1.1rem;
            line-height: 1;
            position: absolute;
            top: 0.5rem;
            right: 0.75rem;
        }
```

- [ ] **Step 2: Update `addTask()` to include reminder fields** — in the task object literal inside `addTask()`, add after `priority: this.currentPriority`:

```js
                    reminderAt: null,
                    reminderFired: false,
```

- [ ] **Step 3: Update the task card template in `render()`** — inside the `tasksList.innerHTML = filteredTasks.map(task => ...)` template, find the `.task-actions` div and update it:

Replace:
```html
                        <div class="task-actions">
                            <button class="btn-delete" onclick="tracker.deleteTask(${task.id})">
                                Delete
                            </button>
                        </div>
```

With:
```html
                        <div class="task-content-wrapper" style="flex:1;min-width:0;">
                            <div class="task-content">
                                <div class="task-title">${this.escapeHtml(task.title)}</div>
                                <div class="task-meta">
                                    <span class="task-meta-item">
                                        📅 Created: ${this.formatDate(task.createdAt)}
                                    </span>
                                    ${task.completedAt ? `
                                        <span class="task-meta-item">
                                            ✅ Completed: ${this.formatDate(task.completedAt)}
                                        </span>
                                    ` : ''}
                                    ${task.duration ? `
                                        <span class="task-meta-item task-duration">
                                            ⏱️ Duration: ${task.duration}
                                        </span>
                                    ` : ''}
                                    ${task.reminderAt && !task.reminderFired ? `
                                        <span class="task-meta-item" style="color:var(--color-success)">
                                            🔔 Reminder: ${this.formatReminderDate(task.reminderAt)}
                                        </span>
                                    ` : ''}
                                    <span class="task-meta-item task-priority">
                                        Priority:
                                        <span class="star-rating">
                                            ${[1, 2, 3, 4, 5].map(i => `
                                                <span class="star ${i <= (task.priority || 3) ? 'filled' : ''}"
                                                      onclick="tracker.setPriority(${task.id}, ${i})">★</span>
                                            `).join('')}
                                        </span>
                                    </span>
                                </div>
                            </div>
                            <div class="reminder-picker" id="reminder-picker-${task.id}">
                                <input type="datetime-local" class="reminder-input"
                                    id="reminder-input-${task.id}"
                                    value="${task.reminderAt ? this.toDatetimeLocal(task.reminderAt) : ''}"
                                >
                                <button class="btn-reminder-save"
                                    onclick="tracker.saveReminder(${task.id})">Set</button>
                                ${task.reminderAt ? `
                                    <button class="btn-reminder-clear"
                                        onclick="tracker.clearReminder(${task.id})">Clear</button>
                                ` : ''}
                            </div>
                        </div>
                        <div class="task-actions">
                            <button class="btn-reminder ${task.reminderAt && !task.reminderFired ? 'has-reminder' : ''}"
                                onclick="tracker.toggleReminderPicker(${task.id})">
                                ${task.reminderAt && !task.reminderFired ? '🔔' : '🔕'} Remind
                            </button>
                            <button class="btn-delete" onclick="tracker.deleteTask(${task.id})">
                                Delete
                            </button>
                        </div>
```

Note: also remove the old `.task-content` div from the template (it's now inside `.task-content-wrapper`).

- [ ] **Step 4: Add reminder helper methods** to the `TaskTracker` class:

```js
            toDatetimeLocal(isoString) {
                const d = new Date(isoString);
                const pad = n => String(n).padStart(2, '0');
                return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
            }

            formatReminderDate(isoString) {
                const d = new Date(isoString);
                return d.toLocaleString('en-IN', { day:'numeric', month:'short', hour:'2-digit', minute:'2-digit' });
            }

            toggleReminderPicker(id) {
                const picker = document.getElementById(`reminder-picker-${id}`);
                if (picker) picker.classList.toggle('open');
            }

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

            clearReminder(id) {
                const task = this.tasks.find(t => t.id === id);
                if (!task) return;
                task.reminderAt = null;
                task.reminderFired = false;
                this.saveTasks();
                this.render();
            }
```

- [ ] **Step 5: Test in browser** — add a task, click "Remind", set a time 1 minute in the future, verify the reminder badge appears on the task card and the datetime is shown in the meta row.

---

### Task 4: In-app notification banner + reminder ticker

**Files:**
- Modify: `TO_do_list_app.html` — add notification banner container, reminder polling loop

- [ ] **Step 1: Add notification banner container** — just before `</body>`, add:

```html
    <div id="notificationBanners" style="position:fixed;top:20px;right:20px;display:flex;flex-direction:column;gap:10px;z-index:9999;max-width:360px;"></div>
```

- [ ] **Step 2: Add notification banner method + reminder scheduler** to `TaskTracker`:

```js
            showBanner(title, body) {
                const container = document.getElementById('notificationBanners');
                const banner = document.createElement('div');
                banner.className = 'notification-banner';
                banner.innerHTML = `
                    <button class="notification-banner-close" onclick="this.parentElement.remove()">✕</button>
                    <div class="notification-banner-title">${title}</div>
                    <div class="notification-banner-body">${body}</div>
                `;
                container.appendChild(banner);
                // Auto-dismiss after 8 seconds
                setTimeout(() => banner.remove(), 8000);
            }

            scheduleReminders() {
                // Clear existing ticker
                if (this._reminderTicker) clearInterval(this._reminderTicker);

                this._reminderTicker = setInterval(() => {
                    const now = new Date();
                    let changed = false;

                    this.tasks.forEach(task => {
                        if (!task.reminderAt || task.reminderFired || task.completed) return;
                        const reminderTime = new Date(task.reminderAt);
                        if (now >= reminderTime) {
                            task.reminderFired = true;
                            changed = true;

                            // In-app banner
                            this.showBanner('🔔 Reminder', `Task: "${task.title}"`);

                            // Browser notification
                            this.sendBrowserNotification(task.title);
                        }
                    });

                    if (changed) {
                        this.saveTasks();
                        this.render();
                    }
                }, 30000); // check every 30 seconds
            }

            async sendBrowserNotification(taskTitle) {
                if (!('Notification' in window)) return;

                if (Notification.permission === 'default') {
                    await Notification.requestPermission();
                }

                if (Notification.permission === 'granted') {
                    const notif = new Notification('Task Reminder 🔔', {
                        body: taskTitle,
                        icon: 'manifest-icon-192.png',
                        badge: 'manifest-icon-192.png',
                        tag: 'task-reminder',
                    });
                    notif.onclick = () => { window.focus(); notif.close(); };
                }
            }
```

- [ ] **Step 3: Call `scheduleReminders()` from `init()`** — add `this.scheduleReminders();` at the end of `init()`.

- [ ] **Step 4: Also fire an immediate check on init** — add this at the end of `init()` (after `scheduleReminders`):

```js
                // Immediate check on load (catches reminders that fired while app was closed)
                setTimeout(() => this._reminderTicker && this._reminderTicker._onTick && this._reminderTicker._onTick(), 500);
```

Actually simpler — extract the check into a named method `checkReminders()` and call it from both `scheduleReminders` and `init`. Update `scheduleReminders` to call `this.checkReminders()` inside the interval, and add a separate `checkReminders()` method containing the forEach logic. Then call `this.checkReminders()` directly in `init()` after `this.scheduleReminders()`.

- [ ] **Step 5: Test** — set a reminder 1 minute ahead, wait, confirm both the in-app banner slides in and a browser OS notification appears (accept the permission prompt when asked).

- [ ] **Step 6: Commit**

```bash
git add "TO_do_list_app.html"
git commit -m "feat: add per-task reminders with in-app banners and browser notifications"
```

---

## Chunk 3: PWA (Home Screen Install)

### Task 5: Create PWA manifest

**Files:**
- Create: `manifest.json`
- Create: `manifest-icon-192.png` (use existing `to-do-list.png` resized, or inline SVG data URI)

- [ ] **Step 1: Create `manifest.json`** in the same folder as `TO_do_list_app.html`:

```json
{
  "name": "Task Tracker",
  "short_name": "Tasks",
  "description": "Track your tasks and to-do lists",
  "start_url": "./TO_do_list_app.html",
  "display": "standalone",
  "background_color": "#0a0e27",
  "theme_color": "#ff00ff",
  "orientation": "portrait-primary",
  "icons": [
    {
      "src": "manifest-icon-192.png",
      "sizes": "192x192",
      "type": "image/png",
      "purpose": "any maskable"
    },
    {
      "src": "manifest-icon-512.png",
      "sizes": "512x512",
      "type": "image/png",
      "purpose": "any maskable"
    }
  ]
}
```

- [ ] **Step 2: Create icon files** — copy or resize `app/src/main/res/drawable/to_do_list.png` to `manifest-icon-192.png` and `manifest-icon-512.png` in the root folder. If Bash/ImageMagick available:

```bash
cp "app/src/main/res/drawable/to_do_list.png" manifest-icon-192.png
cp "app/src/main/res/drawable/to_do_list.png" manifest-icon-512.png
```

(Browsers will scale them; exact resolution isn't critical for basic install.)

- [ ] **Step 3: Link manifest in `TO_do_list_app.html` `<head>`** — add after `<meta name="viewport" ...>`:

```html
    <link rel="manifest" href="manifest.json">
    <meta name="mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
    <meta name="apple-mobile-web-app-title" content="Task Tracker">
    <meta name="theme-color" content="#ff00ff">
    <link rel="apple-touch-icon" href="manifest-icon-192.png">
```

---

### Task 6: Create Service Worker

**Files:**
- Create: `sw.js`
- Modify: `TO_do_list_app.html` — register SW

- [ ] **Step 1: Create `sw.js`** in the same folder as `TO_do_list_app.html`:

```js
const CACHE_NAME = 'task-tracker-v1';
const ASSETS = [
    './',
    './TO_do_list_app.html',
    './manifest.json',
    './manifest-icon-192.png',
    './manifest-icon-512.png',
];

// Install: cache app shell
self.addEventListener('install', event => {
    event.waitUntil(
        caches.open(CACHE_NAME).then(cache => cache.addAll(ASSETS))
    );
    self.skipWaiting();
});

// Activate: clean old caches
self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(keys =>
            Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k)))
        )
    );
    self.clients.claim();
});

// Fetch: serve from cache, fall back to network
self.addEventListener('fetch', event => {
    event.respondWith(
        caches.match(event.request).then(cached => cached || fetch(event.request))
    );
});

// Reminder scheduling via postMessage from page
const pendingReminders = new Map(); // id -> timeoutId

self.addEventListener('message', event => {
    const { type, reminders } = event.data || {};
    if (type !== 'SCHEDULE_REMINDERS') return;

    // Clear all existing timers
    pendingReminders.forEach(tid => clearTimeout(tid));
    pendingReminders.clear();

    const now = Date.now();
    (reminders || []).forEach(({ id, title, reminderAt }) => {
        const delay = new Date(reminderAt).getTime() - now;
        if (delay < 0) return; // already past

        const tid = setTimeout(() => {
            self.registration.showNotification('Task Reminder 🔔', {
                body: title,
                icon: './manifest-icon-192.png',
                badge: './manifest-icon-192.png',
                tag: `reminder-${id}`,
                data: { taskId: id },
            });
        }, delay);

        pendingReminders.set(id, tid);
    });
});

// Notification click: focus the app
self.addEventListener('notificationclick', event => {
    event.notification.close();
    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true }).then(list => {
            const existing = list.find(c => c.url.includes('TO_do_list_app'));
            if (existing) return existing.focus();
            return clients.openWindow('./TO_do_list_app.html');
        })
    );
});
```

- [ ] **Step 2: Register Service Worker in `TO_do_list_app.html`** — add this at the very end of the `<script>` block (after `const tracker = new TaskTracker();`):

```js
        // Register Service Worker (PWA)
        if ('serviceWorker' in navigator) {
            navigator.serviceWorker.register('./sw.js').then(reg => {
                console.log('[SW] Registered', reg.scope);
            }).catch(err => {
                console.warn('[SW] Registration failed', err);
            });
        }
```

- [ ] **Step 3: Send reminders to SW from `scheduleReminders()`** — add this at the end of the `scheduleReminders()` method:

```js
                // Also send to Service Worker for background delivery
                if ('serviceWorker' in navigator && navigator.serviceWorker.controller) {
                    const pending = this.tasks.filter(t => t.reminderAt && !t.reminderFired && !t.completed);
                    navigator.serviceWorker.controller.postMessage({
                        type: 'SCHEDULE_REMINDERS',
                        reminders: pending.map(t => ({ id: t.id, title: t.title, reminderAt: t.reminderAt }))
                    });
                }
```

- [ ] **Step 4: Test PWA install** — serve the folder over a local HTTP server (required for SW):

```bash
# From the app folder:
npx serve . -p 3000
# or: python -m http.server 3000
```

Open `http://localhost:3000/TO_do_list_app.html` in Chrome. Open DevTools → Application → Service Workers — confirm SW is registered. On mobile Chrome, look for "Add to Home Screen" in the browser menu.

- [ ] **Step 5: Test background notification** — with app running via localhost, set a reminder 1 min ahead, switch to a different tab, wait — confirm OS notification fires.

- [ ] **Step 6: Commit**

```bash
git add "TO_do_list_app.html" manifest.json sw.js manifest-icon-192.png manifest-icon-512.png
git commit -m "feat: add PWA manifest and service worker for home screen install and background notifications"
```

---

## Final Verification

- [ ] Open app, cycle through all 6 themes — each renders correctly with no broken layout
- [ ] Reload page — previously selected theme is restored
- [ ] Add a task, set a reminder 1 minute away — badge shows on task card
- [ ] Wait 1 minute — in-app banner slides in from right, OS notification fires
- [ ] Mark task complete before reminder fires — reminder does not fire
- [ ] Serve via localhost, open DevTools Application tab — manifest parsed, SW active, app installable
