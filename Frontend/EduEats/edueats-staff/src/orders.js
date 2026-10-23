// src/orders.js
// Turns the backend's orders into what the staff board shows,
// plus small time helpers.

// The four board columns, in order
export const STATUSES = ["New", "In Progress", "Ready", "Completed"];

// Backend status -> board column (cancelled orders sit under Completed)
const COLUMN_FOR_STATUS = {
  submitted: "New",
  accepted: "New",
  preparing: "In Progress",
  ready: "Ready",
  collected: "Completed",
  completed: "Completed",
  cancelled: "Completed",
};

// The backend cancels orders that stay "Submitted" this long
// (app.orders.expiration-timeout-minutes in application.properties)
export const AUTO_CANCEL_MINUTES = 15;

// Manual orders store the walk-in customer's name at the start of the
// first item's note, e.g. "Walk-in: Thabo | no onions"
export const WALK_IN_PREFIX = "Walk-in: ";
const WALK_IN_SEPARATOR = " | ";

export function walkInNote(customerName, note) {
  const name = customerName.trim();
  const rest = note.trim();
  if (!name) return rest;
  return rest ? `${WALK_IN_PREFIX}${name}${WALK_IN_SEPARATOR}${rest}` : `${WALK_IN_PREFIX}${name}`;
}

function readWalkIn(note) {
  if (!note || !note.startsWith(WALK_IN_PREFIX)) return { name: "", note: note || "" };
  const rest = note.slice(WALK_IN_PREFIX.length);
  const split = rest.indexOf(WALK_IN_SEPARATOR);
  return split === -1
    ? { name: rest, note: "" }
    : { name: rest.slice(0, split), note: rest.slice(split + WALK_IN_SEPARATOR.length) };
}

export function toBoardOrder(order) {
  const statusKey = String(order.status || "Submitted").trim().toLowerCase();
  let walkInName = "";

  const lines = (order.items || []).map((item, index) => {
    let note = item.specialInstructions || "";
    if (index === 0) {
      const walkIn = readWalkIn(note);
      walkInName = walkIn.name;
      note = walkIn.note;
    }
    return {
      quantity: item.quantity,
      name: item.menuItemName,
      extras: item.selectedModifiers || [],
      note,
    };
  });

  const customer = walkInName || order.customerName || `Order #${order.orderId}`;
  const id = order.customerOrderNumber || `#${order.orderId}`;

  return {
    id,
    backendId: order.orderId,
    backendStatus: order.status || "Submitted",
    statusKey,
    status: COLUMN_FOR_STATUS[statusKey],
    cancelled: statusKey.includes("cancel"),
    customer,
    walkIn: Boolean(walkInName),
    lines,
    // Everything the search box looks through
    searchText: [
      id,
      customer,
      ...lines.map((line) => `${line.name} ${line.extras.join(" ")} ${line.note}`),
    ]
      .join(" ")
      .toLowerCase(),
    total: Number(order.totalPrice ?? 0),
    createdAt: order.createdAt,
  };
}

// Short summary of the items, e.g. for the list view
export const itemsSummary = (order) =>
  order.lines
    .map((line) => {
      const extras = [...line.extras, line.note ? `"${line.note}"` : ""].filter(Boolean);
      return `${line.quantity}x ${line.name}${extras.length ? ` (${extras.join(", ")})` : ""}`;
    })
    .join(", ");

export function minutesSince(dateValue, now) {
  const time = new Date(dateValue).getTime();
  return Number.isNaN(time) ? 0 : Math.max(0, Math.floor((now - time) / 60000));
}

export function timeAgo(dateValue, now) {
  if (!dateValue) return "";
  const minutes = minutesSince(dateValue, now);

  if (minutes < 1) return "just now";
  if (minutes < 60) return `${minutes}m ago`;

  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;

  return `${Math.floor(hours / 24)}d ago`;
}

// "12:04" for today, "14 Oct 12:04" for older orders
export function clockTime(dateValue, now) {
  const date = new Date(dateValue);
  if (Number.isNaN(date.getTime())) return "";
  const time = date.toLocaleTimeString("en-ZA", { hour: "2-digit", minute: "2-digit" });
  return isToday(dateValue, now)
    ? time
    : `${date.toLocaleDateString("en-ZA", { day: "numeric", month: "short" })} ${time}`;
}

export function isToday(dateValue, now = Date.now()) {
  if (!dateValue) return false;
  const date = new Date(dateValue);
  if (Number.isNaN(date.getTime())) return false;
  const today = new Date(now);
  return (
    date.getFullYear() === today.getFullYear() &&
    date.getMonth() === today.getMonth() &&
    date.getDate() === today.getDate()
  );
}

export const formatPrice = (price) => `R${Number(price || 0).toFixed(2)}`;
