// src/components/OrderAlertBanner.jsx
// Bar shown on the Menu, Cart and Profile pages when something happens to
// an order (My Orders has its own messages):
// - green: "Your order is ready!"
// - red: "Your order was cancelled" (e.g. not accepted in time)
// "View" opens My Orders.

import "./OrderAlertBanner.css";
import { cancelMessage } from "../orderStatus";

function OrderAlertBanner({ alert, onView, onClose }) {
  const { kind, order } = alert;
  const number = order.customerOrderNumber || `#${order.orderId}`;

  return (
    <div className={`order-alert ${kind}`} role="alert">
      <span className="order-alert-text">
        {kind === "ready" ? (
          <>
            🍽️ Your order <strong>{number}</strong> is ready! Collect it at the counter.
          </>
        ) : (
          <>⚠️ {cancelMessage(order)}</>
        )}
      </span>
      <button type="button" className="order-alert-view" onClick={onView}>
        View
      </button>
      <button type="button" className="order-alert-close" onClick={onClose} aria-label="Close message">
        ×
      </button>
    </div>
  );
}

export default OrderAlertBanner;
