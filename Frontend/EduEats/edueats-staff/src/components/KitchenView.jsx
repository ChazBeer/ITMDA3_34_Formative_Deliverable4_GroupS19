// src/components/KitchenView.jsx
// Full-screen kitchen display for a tablet or TV in the kitchen.
// Big cards for orders to start, orders being made and orders waiting at the
// counter, oldest first, with one big button each. Live, like the board.
// Escape or "Exit" goes back to the normal board.

import { useEffect, useRef } from "react";
import "./KitchenView.css";
import ItemThumb from "./ItemThumb";
import { AUTO_CANCEL_MINUTES, isToday, minutesSince } from "../orders";

const LANES = [
  { status: "New", title: "To start", button: "Start", next: "In Progress" },
  { status: "In Progress", title: "Cooking", button: "Ready", next: "Ready" },
  { status: "Ready", title: "At the counter", button: "Collected", next: "Completed" },
];

function KitchenView({ orders, now, live, updatingId, findItem, onAdvance, onExit }) {
  const exitRef = useRef(onExit);
  useEffect(() => {
    exitRef.current = onExit;
  }, [onExit]);

  // Full screen if the browser allows it; Escape exits
  useEffect(() => {
    const element = document.documentElement;
    if (element.requestFullscreen && !document.fullscreenElement) {
      element.requestFullscreen().catch(() => {
        // not allowed (e.g. inside a frame) - the view still fills the window
      });
    }

    const onKey = (event) => {
      if (event.key === "Escape") exitRef.current();
    };
    window.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";

    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
      if (document.fullscreenElement && document.exitFullscreen) {
        document.exitFullscreen().catch(() => {});
      }
    };
  }, []);

  const clock = new Date(now).toLocaleTimeString("en-ZA", { hour: "2-digit", minute: "2-digit" });

  return (
    <div className="kitchen-view" role="dialog" aria-modal="true" aria-label="Kitchen display">
      <header className="kitchen-header">
        <div className="kitchen-brand">
          <span className="brand-logo">E</span>
          Kitchen display
        </div>
        <span className={`kitchen-live ${live ? "on" : ""}`}>
          ● {live ? "Live" : "Checking every 15 s"}
        </span>
        <span className="kitchen-clock">{clock}</span>
        <button type="button" className="kitchen-exit" onClick={onExit}>
          Exit
        </button>
      </header>

      <div className="kitchen-lanes">
        {LANES.map((lane) => {
          const laneOrders = orders
            .filter(
              (order) =>
                order.status === lane.status && !order.cancelled && isToday(order.createdAt, now)
            )
            .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt)); // oldest first

          return (
            <section className={`kitchen-lane lane-${lane.status.replace(" ", "-").toLowerCase()}`} key={lane.status}>
              <h2>
                {lane.title} <span>{laneOrders.length}</span>
              </h2>

              <div className="kitchen-cards">
                {laneOrders.map((order) => {
                  const waited = minutesSince(order.createdAt, now);
                  const late =
                    order.statusKey === "submitted" && waited >= AUTO_CANCEL_MINUTES - 5;

                  return (
                    <article className={`kitchen-card ${late ? "late" : ""}`} key={order.id}>
                      <div className="kitchen-card-top">
                        <strong>{order.id.replace(/^ORD-/, "")}</strong>
                        <span className="kitchen-wait">{waited} min</span>
                      </div>
                      <div className="kitchen-customer">{order.customer}</div>

                      <ul className="kitchen-lines">
                        {order.lines.map((line, index) => (
                          <li key={index}>
                            <ItemThumb item={findItem(line.name) || { name: line.name }} />
                            <div>
                              <b>
                                {line.quantity}× {line.name}
                              </b>
                              {line.extras.length > 0 && <small>{line.extras.join(", ")}</small>}
                              {line.note && <small className="kitchen-note">⚠ {line.note}</small>}
                            </div>
                          </li>
                        ))}
                      </ul>

                      <button
                        type="button"
                        className="kitchen-next"
                        disabled={updatingId === order.id}
                        onClick={() => onAdvance(order.id, lane.next)}
                      >
                        {lane.button} →
                      </button>
                    </article>
                  );
                })}

                {laneOrders.length === 0 && <p className="kitchen-empty">Nothing here</p>}
              </div>
            </section>
          );
        })}
      </div>
    </div>
  );
}

export default KitchenView;
