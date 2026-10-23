// src/pages/Orders/Orders.jsx
// My Orders: progress tracker, cancel, QR code for the counter,
// silent auto-refresh and a "Your order is ready!" alert.

import { useEffect, useRef, useState } from "react";
import QRCode from "react-qr-code";
import "./Orders.css";
import { errorFromBody } from "../../apiError";
import { apiFetch } from "../../api";
import AppHeader from "../../components/AppHeader";
import ConfirmDialog from "../../components/ConfirmDialog";
import useOrderUpdates from "../../useOrderUpdates";
import NotifyPrompt from "../../components/NotifyPrompt";
import { orderLineTotal } from "../../orderLine";
import {
  normalise,
  isCancelled,
  isReady,
  isFinished,
  cancelMessage,
  markCancelledByStudent,
  wasCancelledByStudent,
} from "../../orderStatus";

// The order goes through these steps (same names as the backend)
const STEPS = ["Submitted", "Accepted", "Preparing", "Ready", "Collected"];

// Backup check for updates while the tab is open.
// Live updates (WebSocket) normally do the work, so the backup runs
// slowly while they're connected and quickly if the connection drops.
const REFRESH_LIVE_MS = 60000;
const REFRESH_OFFLINE_MS = 15000;

// The backend allows cancelling until the kitchen starts preparing
const canCancel = (status) => ["submitted", "accepted"].includes(normalise(status));

const getStepIndex = (status) => {
  const index = STEPS.findIndex((step) => step.toLowerCase() === normalise(status));
  return index === -1 ? 0 : index;
};

const getStatusClass = (status) => {
  const value = normalise(status);
  if (value.includes("collect")) return "status-completed";
  if (value === "ready") return "status-ready";
  if (value.includes("prepar") || value.includes("accept")) return "status-active";
  if (value.includes("cancel")) return "status-cancelled";
  return "status-pending";
};

const formatDate = (dateValue) => {
  if (!dateValue) return "";
  return new Date(dateValue).toLocaleString("en-ZA", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const formatPrice = (price) => `R ${Number(price || 0).toFixed(2)}`;

const getOrderNumber = (order) => order.customerOrderNumber || `#${order.orderId}`;

// What goes inside the QR code: the order number the staff can look up
const getQrValue = (order) =>
  order.customerOrderNumber || `EDUEATS-ORDER-${order.orderId}`;

// ---------- Small pieces ----------

function StatusTracker({ status }) {
  if (isCancelled(status)) {
    return <p className="tracker-cancelled">This order was cancelled.</p>;
  }

  const current = getStepIndex(status);

  return (
    <ol className="tracker" aria-label={`Order progress: ${status}`}>
      {STEPS.map((step, index) => {
        let state = "todo";
        if (index < current) state = "done";
        if (index === current) state = "current";

        return (
          <li key={step} className={`tracker-step ${state}`}>
            <span className="tracker-dot">{index < current ? "✓" : index + 1}</span>
            <span className="tracker-label">{step}</span>
          </li>
        );
      })}
    </ol>
  );
}

function CounterView({ order, onClose }) {
  // Close with the Escape key on a laptop
  useEffect(() => {
    const onKey = (event) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  return (
    <div className="counter-overlay" role="dialog" aria-modal="true" aria-label="Show at counter">
      <div className="counter-card">
        <p className="counter-hint">Show this at the counter</p>

        <div className="counter-qr">
          <QRCode value={getQrValue(order)} size={240} style={{ width: "100%", height: "auto" }} />
        </div>

        <h2 className="counter-number">{getOrderNumber(order)}</h2>

        <span className={`order-status ${getStatusClass(order.status)}`}>
          {order.status || "Submitted"}
        </span>

        <p className="counter-total">
          Amount to pay: <strong>{formatPrice(order.totalPrice)}</strong>
        </p>

        <button className="counter-close" onClick={onClose}>
          Done
        </button>
      </div>
    </div>
  );
}

// ---------- Page ----------

function Orders({
  user,
  token,
  cartCount = 0,
  cartAlert = false,
  onCartSeen,
  onNavigate,
  onLogout,
}) {
  const goToMenu = () => onNavigate("menu");

  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [lastUpdated, setLastUpdated] = useState(null);

  const [counterOrder, setCounterOrder] = useState(null);
  const [cancellingId, setCancellingId] = useState(null);
  const [confirmCancel, setConfirmCancel] = useState(null); // order waiting for "Yes, cancel"
  const [notice, setNotice] = useState(null); // { type: "ready" | "info" | "error", text }

  const loadingRef = useRef(false); // stops two loads running at the same time
  const reloadAgainRef = useRef(false); // an update arrived while a load was running
  const statusesRef = useRef(null); // last known status of each order

  // orderIds: for "ready" / "cancelled" notices, which orders they are about
  const showNotice = (type, text, orderIds = []) => {
    setNotice({ type, text, orderIds, id: Date.now() });
  };

  // Hide info/error notices after a few seconds
  // ("ready" and "cancelled" stay until closed - the student must see them)
  useEffect(() => {
    if (!notice || notice.type === "ready" || notice.type === "cancelled") return undefined;
    const timer = setTimeout(() => setNotice(null), 4000);
    return () => clearTimeout(timer);
  }, [notice]);

  // Compare old and new statuses and alert if something became Ready
  const checkForReadyOrders = (newOrders) => {
    const previous = statusesRef.current;
    const next = {};

    newOrders.forEach((order) => {
      next[order.orderId] = order.status;
    });

    // First load: just remember the statuses, don't alert
    if (previous) {
      const nowReady = newOrders.filter(
        (order) => isReady(order.status) && !isReady(previous[order.orderId])
      );

      if (nowReady.length > 0) {
        const numbers = nowReady.map(getOrderNumber).join(", ");
        showNotice(
          "ready",
          `Your order ${numbers} is ready! Please collect it at the counter.`,
          nowReady.map((order) => order.orderId)
        );

        if (navigator.vibrate) {
          navigator.vibrate([200, 100, 200]);
        }
      }

      // Cancelled by the cafeteria (or not accepted in time), not by the student
      const nowCancelled = newOrders.filter(
        (order) =>
          isCancelled(order.status) &&
          previous[order.orderId] !== undefined &&
          !isCancelled(previous[order.orderId]) &&
          !wasCancelledByStudent(order.orderId)
      );

      if (nowCancelled.length > 0 && nowReady.length === 0) {
        showNotice(
          "cancelled",
          cancelMessage(nowCancelled[0]),
          nowCancelled.map((order) => order.orderId)
        );
      }
    }

    statusesRef.current = next;
  };

  // silent = true means update in the background without the loading screen
  const loadOrders = async (silent = false) => {
    if (!user?.userId) {
      setError("Your user account could not be identified.");
      setLoading(false);
      return;
    }

    if (loadingRef.current) {
      // Don't lose it: load once more as soon as this one finishes
      reloadAgainRef.current = true;
      return;
    }
    loadingRef.current = true;

    try {
      if (!silent) {
        setLoading(true);
        setError("");
      }

      const response = await apiFetch(`/api/orders/user/${user.userId}`);

      // Still 401 after trying to refresh - session is over, back to login
      if (response.status === 401) {
        if (onLogout) onLogout();
        return;
      }

      if (!response.ok) {
        throw new Error(`Could not load orders. Server returned ${response.status}.`);
      }

      const data = await response.json();

      // Newest orders first
      const sortedOrders = [...data].sort(
        (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
      );

      checkForReadyOrders(sortedOrders);
      setOrders(sortedOrders);

      // The "ready" banner goes away by itself once those orders are
      // collected or cancelled (no longer Ready)
      setNotice((current) => {
        if (current?.type !== "ready" || !current.orderIds?.length) return current;
        const stillReady = sortedOrders.some(
          (order) => current.orderIds.includes(order.orderId) && isReady(order.status)
        );
        return stillReady ? current : null;
      });
      setError("");
      setLastUpdated(new Date());
    } catch (err) {
      console.error("Order loading error:", err);

      // A failed background check shouldn't wipe the orders already on screen
      if (!silent) {
        setError(err.message || "Could not load your orders.");
      }
    } finally {
      loadingRef.current = false;
      setLoading(false);

      if (reloadAgainRef.current) {
        reloadAgainRef.current = false;
        loadOrders(true);
      }
    }
  };

  // NEW: live updates. When staff change one of this student's orders, the
  // backend pushes it here straight away, so we reload the list at once
  // (the backup check below still runs, just less often).
  const live = useOrderUpdates(user?.userId, () => loadOrders(true));

  // First load (and again if the user or token changes).
  // Started straight after the page appears, not during it, so React
  // doesn't have to redraw twice in a row.
  useEffect(() => {
    statusesRef.current = null;
    const timer = setTimeout(() => loadOrders(), 0);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.userId, token]);

  // Auto-refresh while the tab is visible, and straight away when you come back to it
  useEffect(() => {
    const timer = setInterval(() => {
      if (document.visibilityState === "visible") {
        loadOrders(true);
      }
    }, live ? REFRESH_LIVE_MS : REFRESH_OFFLINE_MS);

    const onVisible = () => {
      if (document.visibilityState === "visible") {
        loadOrders(true);
      }
    };

    document.addEventListener("visibilitychange", onVisible);
    window.addEventListener("online", onVisible);

    return () => {
      clearInterval(timer);
      document.removeEventListener("visibilitychange", onVisible);
      window.removeEventListener("online", onVisible);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.userId, token, live]);

  // Asks the server for this student's orders and checks if one is cancelled
  const checkIfCancelled = async (orderId) => {
    try {
      const response = await apiFetch(`/api/orders/user/${user.userId}`);
      if (!response.ok) return false;

      const data = await response.json();
      return data.some(
        (item) => item.orderId === orderId && isCancelled(item.status)
      );
    } catch {
      return false;
    }
  };

  // Step 1: "Cancel order" opens the app's own pop-up
  const askToCancel = (order) => setConfirmCancel(order);

  // Step 2: the student pressed "Yes, cancel"
  const cancelOrder = async (order) => {
    setConfirmCancel(null);
    markCancelledByStudent(order.orderId); // no "the cafeteria cancelled it" alert

    try {
      setCancellingId(order.orderId);

      const response = await apiFetch(`/api/orders/${order.orderId}/cancel`, {
        method: "PUT",
      });

      if (response.status === 401) {
        if (onLogout) onLogout();
        return;
      }

      if (!response.ok) {
        let message = `Could not cancel the order (server returned ${response.status}).`;
        try {
          const body = await response.json();
          message = errorFromBody(body) || message;
        } catch {
          // no JSON body - keep the default message
        }

        // Double-check: sometimes the server cancels the order but still
        // replies with an error. If it really is cancelled, treat it as success.
        const reallyCancelled = await checkIfCancelled(order.orderId);

        if (!reallyCancelled) {
          throw new Error(message);
        }

        console.warn(
          `Cancel returned ${response.status} but the order is cancelled:`,
          message
        );
      }

      // Show it as cancelled straight away, then get the real data
      setOrders((current) =>
        current.map((item) =>
          item.orderId === order.orderId ? { ...item, status: "Cancelled" } : item
        )
      );
      showNotice("info", `Order ${getOrderNumber(order)} was cancelled.`);
      loadOrders(true);
    } catch (err) {
      console.error("Cancel error:", err);
      showNotice("error", err.message || "Could not cancel the order.");
      loadOrders(true);
    } finally {
      setCancellingId(null);
    }
  };

  const activeOrders = orders.filter((order) => !isFinished(order.status));
  const pastOrders = orders.filter((order) => isFinished(order.status));

  const renderOrderCard = (order, index) => {
    const ready = isReady(order.status);
    const finished = isFinished(order.status);

    return (
      <article
        className={`order-card ${ready ? "order-ready" : ""} ${finished ? "order-finished" : ""}`}
        key={order.orderId}
        style={{ animationDelay: `${Math.min(index, 8) * 40}ms` }}
      >
        <div className="order-card-header">
          <div className="order-heading">
            <span className="order-label">ORDER</span>
            <h3>{getOrderNumber(order)}</h3>
            <p>{formatDate(order.createdAt)}</p>
          </div>

          <span className={`order-status ${getStatusClass(order.status)}`}>
            {order.status || "Submitted"}
          </span>
        </div>

        {!finished && <StatusTracker status={order.status} />}

        {isCancelled(order.status) && !wasCancelledByStudent(order.orderId) && (
          <p className="cancel-reason">{cancelMessage(order)}</p>
        )}

        {ready && (
          <p className="ready-callout">Ready! Collect it at the counter and pay at the till.</p>
        )}

        <div className="order-items">
          {order.items?.length > 0 ? (
            order.items.map((item, itemIndex) => (
              <div className="order-item" key={`${order.orderId}-${itemIndex}`}>
                <div className="order-item-info">
                  <strong>
                    {item.quantity} × {item.menuItemName}
                  </strong>

                  {item.selectedModifiers?.length > 0 && (
                    <p>{item.selectedModifiers.join(", ")}</p>
                  )}

                  {item.specialInstructions && (
                    <p className="special-instructions">Note: {item.specialInstructions}</p>
                  )}
                </div>

                <span>
                  {formatPrice(orderLineTotal(item))}
                </span>
              </div>
            ))
          ) : (
            <p>No item information available.</p>
          )}
        </div>

        <div className="order-total">
          <span>Total</span>
          <strong>{formatPrice(order.totalPrice)}</strong>
        </div>

        {!finished && (
          <div className="order-actions">
            <button className="counter-button" onClick={() => setCounterOrder(order)}>
              <span className="mini-qr" aria-hidden="true">
                <QRCode value={getQrValue(order)} size={28} />
              </span>
              Show at counter
            </button>

            {canCancel(order.status) && (
              <button
                className="cancel-button"
                onClick={() => askToCancel(order)}
                disabled={cancellingId === order.orderId}
              >
                {cancellingId === order.orderId ? "Cancelling..." : "Cancel order"}
              </button>
            )}
          </div>
        )}
      </article>
    );
  };

  return (
    <div className="orders-page">
      {/* Shared top bar + ☰ side menu + Help */}
      <AppHeader
        user={user}
        current="orders"
        cartCount={cartCount}
        cartAlert={cartAlert}
        onCartSeen={onCartSeen}
        onNavigate={onNavigate}
        onLogout={onLogout}
      />

      {notice && (
        <div className={`orders-notice notice-${notice.type}`} role="status" key={notice.id}>
          <span>{notice.text}</span>
          <button onClick={() => setNotice(null)} aria-label="Close message">
            ×
          </button>
        </div>
      )}

      <main className="orders-content page-enter">
        <section className="orders-title-section">
          <h2>My Orders</h2>
          <p className={`live-indicator ${live ? "is-live" : ""}`}>
            <span className="live-dot" aria-hidden="true" />
            {live ? "Live updates" : "Updates automatically"}
            {lastUpdated &&
              ` · ${lastUpdated.toLocaleTimeString("en-ZA", {
                hour: "2-digit",
                minute: "2-digit",
              })}`}
          </p>
        </section>

        {loading && (
          <div className="orders-list">
            {[1, 2].map((n) => (
              <div className="order-card skeleton-card" key={n}>
                <div className="skeleton-line short" />
                <div className="skeleton-line" />
                <div className="skeleton-line" />
              </div>
            ))}
          </div>
        )}

        {!loading && error && (
          <div className="orders-message error-message">
            <h3>Unable to load orders</h3>
            <p>{error}</p>
            <button onClick={() => loadOrders()}>Try Again</button>
          </div>
        )}

        {!loading && !error && orders.length === 0 && (
          <div className="orders-message">
            <h3>No orders yet</h3>
            <p>Once you place an order from the menu, it will appear here.</p>
            <button onClick={goToMenu}>Browse Menu</button>
          </div>
        )}

        {!loading && !error && orders.length > 0 && (
          <>
            <section className="orders-section">
              <h3 className="orders-section-title">Current orders</h3>
              {activeOrders.length > 0 ? (
                <>
                  <div className="orders-list">{activeOrders.map(renderOrderCard)}</div>
                  <NotifyPrompt />
                </>
              ) : (
                <p className="orders-section-empty">
                  Nothing in progress right now.{" "}
                  <button className="link-button" onClick={goToMenu}>
                    Order something
                  </button>
                </p>
              )}
            </section>

            {pastOrders.length > 0 && (
              <section className="orders-section">
                <h3 className="orders-section-title">Past orders</h3>
                <div className="orders-list">{pastOrders.map(renderOrderCard)}</div>
              </section>
            )}
          </>
        )}
      </main>

      {counterOrder && (
        <CounterView
          order={orders.find((o) => o.orderId === counterOrder.orderId) || counterOrder}
          onClose={() => setCounterOrder(null)}
        />
      )}

      {confirmCancel && (
        <ConfirmDialog
          danger
          title="Cancel this order?"
          message={`Order ${getOrderNumber(confirmCancel)} will be cancelled and the cafeteria won't make it. This can't be undone.`}
          confirmLabel="Yes, cancel"
          cancelLabel="Keep order"
          onConfirm={() => cancelOrder(confirmCancel)}
          onCancel={() => setConfirmCancel(null)}
        />
      )}
    </div>
  );
}

export default Orders;
