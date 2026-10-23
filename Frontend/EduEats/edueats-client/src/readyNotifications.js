// src/readyNotifications.js
// Phone/laptop notifications about an order while EduEats is open in the
// background (e.g. the student switched to WhatsApp):
// "Your order is ready!" and "Your order was cancelled".
//
// Limits (browser rules, not ours):
// - Needs a secure address: localhost or https (not http://192.168.x.x)
// - Works on Android and laptops; iPhones only if EduEats is installed
//   to the Home Screen
// - The app must still be open somewhere; a fully closed browser would
//   need real push notifications from the backend

import { cancelMessage } from "./orderStatus";

const ICON = "/pwa-192x192.png";

export function canNotify() {
  return typeof window !== "undefined" && "Notification" in window && window.isSecureContext;
}

// "default" = not asked yet, "granted", "denied", or "unsupported"
export function notificationPermission() {
  return canNotify() ? Notification.permission : "unsupported";
}

export async function askNotificationPermission() {
  if (!canNotify()) return "unsupported";
  try {
    return await Notification.requestPermission();
  } catch {
    return Notification.permission;
  }
}

async function show(title, body, tag) {
  if (notificationPermission() !== "granted") return;

  const options = { body, icon: ICON, badge: ICON, tag, data: { url: "/#orders" } };

  try {
    // Phones need the app's background helper (service worker) to show it
    const registration = await navigator.serviceWorker?.getRegistration?.();
    if (registration) {
      await registration.showNotification(title, options);
      return;
    }

    // Laptop while developing (no service worker): show it directly
    const notification = new Notification(title, options);
    notification.onclick = () => {
      window.focus();
      window.location.hash = "orders";
      notification.close();
    };
  } catch (error) {
    console.warn("Could not show the notification:", error);
  }
}

const orderNumber = (order) =>
  order?.customerOrderNumber || (order?.orderId ? `#${order.orderId}` : "");

export function showReadyNotification(order) {
  const number = orderNumber(order);
  return show(
    "Your order is ready! 🍽️",
    `${number ? `Order ${number}` : "Your order"} is waiting at the counter. Pay at the till when you collect.`,
    `edueats-ready-${order?.orderId ?? "order"}` // never shown twice for one order
  );
}

export function showCancelledNotification(order) {
  return show(
    "Your order was cancelled",
    cancelMessage(order),
    `edueats-cancelled-${order?.orderId ?? "order"}`
  );
}
