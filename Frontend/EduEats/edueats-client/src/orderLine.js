// src/orderLine.js
// Price of one line of a placed order (e.g. "2 × Burger + Cheese").
// The backend now sends the line's full price (item + extras, × quantity).
// Older orders without it fall back to base price × quantity.

const LINE_TOTAL_FIELDS = ["lineTotal"];

export function orderLineTotal(item) {
  for (const field of LINE_TOTAL_FIELDS) {
    if (item?.[field] != null) return Number(item[field]);
  }
  return Number(item?.priceAtPurchase || 0) * Number(item?.quantity || 0);
}
