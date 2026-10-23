// src/orderStatus.js
// Small helpers for order statuses (same names as the backend),
// shared by My Orders, the app-wide "order ready" alert and the idle timer.

export const normalise = (status) => String(status || "").trim().toLowerCase();

export const isCancelled = (status) => normalise(status).includes("cancel");
export const isCollected = (status) => normalise(status).includes("collect");
export const isReady = (status) => normalise(status) === "ready";

// Collected or cancelled = nothing more will happen to it
export const isFinished = (status) => isCancelled(status) || isCollected(status);

// ---------- Cancelled orders: why? ----------

// Orders the student cancelled themselves on this device (no alert needed).
// Remembered in the browser, so it still knows after a refresh.
const CANCELLED_BY_STUDENT_KEY = "edueatsCancelledByMe";

function readCancelledByStudent() {
  try {
    const saved = JSON.parse(localStorage.getItem(CANCELLED_BY_STUDENT_KEY));
    return Array.isArray(saved) ? saved : [];
  } catch {
    return [];
  }
}

export function markCancelledByStudent(orderId) {
  const ids = readCancelledByStudent().filter((id) => id !== orderId);
  ids.push(orderId);
  try {
    // only the latest 50 are needed
    localStorage.setItem(CANCELLED_BY_STUDENT_KEY, JSON.stringify(ids.slice(-50)));
  } catch {
    // not saved - worst case the card shows the general "cancelled" message
  }
}

export const wasCancelledByStudent = (orderId) => readCancelledByStudent().includes(orderId);

// The backend cancels orders that stay "Submitted" for 15 minutes
const EXPIRY_MINUTES = 15;

// "expired" = the cafeteria didn't accept it in time, "cafeteria" = staff
// cancelled it. Uses the backend's reason if it sends one, otherwise the
// order's age (cancelled 15+ minutes after it was placed = expired).
export function cancelReason(order) {
  const reason = String(order?.cancelReason ?? order?.cancellationReason ?? "").toLowerCase();
  if (reason.includes("expire") || reason.includes("timeout")) return "expired";
  if (reason) return "cafeteria";

  const placed = new Date(order?.createdAt).getTime();
  const minutesOld = (Date.now() - placed) / 60000;
  return Number.isFinite(minutesOld) && minutesOld >= EXPIRY_MINUTES - 1 ? "expired" : "cafeteria";
}

export function cancelMessage(order) {
  const number = order?.customerOrderNumber || (order?.orderId ? `#${order.orderId}` : "");
  return cancelReason(order) === "expired"
    ? `Order ${number} was cancelled because the cafeteria didn't accept it in time. You haven't been charged, so you can order again.`
    : `Order ${number} was cancelled by the cafeteria. You haven't been charged. Ask at the counter if you're unsure why.`;
}
