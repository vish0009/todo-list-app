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
        caches.open(CACHE_NAME)
            .then(cache => cache.addAll(ASSETS.filter(a => !a.endsWith('.png') || true)))
            .catch(() => {}) // icons may not exist yet
    );
    self.skipWaiting();
});

// Activate: remove old caches
self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(keys =>
            Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k)))
        )
    );
    self.clients.claim();
});

// Fetch: cache-first for app assets, network-first otherwise
self.addEventListener('fetch', event => {
    if (event.request.method !== 'GET') return;
    event.respondWith(
        caches.match(event.request).then(cached => cached || fetch(event.request))
    );
});

// ── Reminder Scheduling via postMessage ──
const pendingReminders = new Map(); // taskId -> timeoutId

self.addEventListener('message', event => {
    const { type, reminders } = event.data || {};
    if (type !== 'SCHEDULE_REMINDERS') return;

    // Clear old timers
    pendingReminders.forEach(tid => clearTimeout(tid));
    pendingReminders.clear();

    const now = Date.now();
    (reminders || []).forEach(({ id, title, reminderAt }) => {
        const delay = new Date(reminderAt).getTime() - now;
        if (delay <= 0) return; // already past

        const tid = setTimeout(() => {
            self.registration.showNotification('Task Reminder 🔔', {
                body: title,
                icon: './manifest-icon-192.png',
                badge: './manifest-icon-192.png',
                tag: `reminder-${id}`,
                data: { taskId: id },
                requireInteraction: false,
            });
            pendingReminders.delete(id);
        }, delay);

        pendingReminders.set(id, tid);
    });
});

// Notification click: focus or open the app
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
