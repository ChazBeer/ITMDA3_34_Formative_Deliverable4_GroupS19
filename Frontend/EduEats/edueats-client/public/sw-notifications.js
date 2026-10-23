// public/sw-notifications.js
// Added to the app's service worker (see vite.config.js, workbox.importScripts).
// Tapping the "Your order is ready!" notification opens My Orders.

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const url = new URL(event.notification.data?.url || "/#orders", self.location.origin).href;

  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((windows) => {
      const open = windows.find((client) => client.url.startsWith(self.location.origin));
      if (open) {
        return open.focus().then((client) => client.navigate?.(url));
      }
      return self.clients.openWindow(url);
    })
  );
});
