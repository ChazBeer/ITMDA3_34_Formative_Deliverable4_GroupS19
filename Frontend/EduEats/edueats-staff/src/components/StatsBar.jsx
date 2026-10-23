// src/components/StatsBar.jsx
// Today at a glance, above the board: orders, sales, waiting, ready.
// Worked out from today's orders (cancelled orders don't count as sales).

import { formatPrice, isToday, minutesSince } from "../orders";

function StatsBar({ orders, now }) {
  const today = orders.filter((order) => isToday(order.createdAt, now));
  const placed = today.filter((order) => !order.cancelled);
  const cancelled = today.length - placed.length;

  const sales = placed.reduce((sum, order) => sum + order.total, 0);
  const collected = placed.filter((order) => order.statusKey === "collected");

  const waiting = placed.filter((order) => order.status === "New");
  const oldestWait = waiting.length
    ? Math.max(...waiting.map((order) => minutesSince(order.createdAt, now)))
    : 0;

  const ready = placed.filter((order) => order.status === "Ready");

  const tiles = [
    {
      label: "Orders today",
      value: placed.length,
      note: cancelled ? `${cancelled} cancelled` : "None cancelled",
    },
    {
      label: "Sales today",
      value: formatPrice(sales),
      note: `${formatPrice(collected.reduce((sum, order) => sum + order.total, 0))} collected`,
    },
    {
      label: "Waiting to start",
      value: waiting.length,
      note: waiting.length ? `Oldest: ${oldestWait} min` : "All caught up",
      alert: oldestWait >= 10,
    },
    {
      label: "Ready for pickup",
      value: ready.length,
      note: ready.length ? "Waiting at the counter" : "Nothing waiting",
    },
  ];

  return (
    <section className="stats-bar" aria-label="Today at a glance">
      {tiles.map((tile) => (
        <div className={`stat-tile ${tile.alert ? "alert" : ""}`} key={tile.label}>
          <span className="stat-label">{tile.label}</span>
          <strong className="stat-value">{tile.value}</strong>
          <span className="stat-note">{tile.note}</span>
        </div>
      ))}
    </section>
  );
}

export default StatsBar;
