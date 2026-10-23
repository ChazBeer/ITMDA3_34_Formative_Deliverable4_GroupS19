import { useEffect, useState } from "react";
import "./UserProfile.css";
import "./AdminsPage.css";
import ConfirmDialog from "../components/ConfirmDialog";
import { checkNewPassword, PASSWORD_HINT } from "../passwordRules";
import {
  loadOrdersForUser,
  formatRand,
  formatDate,
  setSuspended,
  resetPassword,
} from "./adminData";

// CHANGED: shows the user's real orders, and Suspend / Reactivate and
// Reset Password now work. Admin accounts can't be suspended (backend rule),
// and admins can't reset their own password here.
function UserProfile({ user, currentUserId, onBack, onUserChanged }) {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // NEW: actions
  const [confirmingSuspend, setConfirmingSuspend] = useState(false);
  const [saving, setSaving] = useState(false);
  const [actionError, setActionError] = useState("");
  const [notice, setNotice] = useState("");
  const [resetOpen, setResetOpen] = useState(false);

  // Orders load again only when a different user is opened
  const userId = user?.id;

  useEffect(() => {
    if (!userId) return undefined;

    let cancelled = false;

    loadOrdersForUser(userId)
      .then((data) => {
        if (cancelled) return;
        setOrders(
          [...data].sort(
            (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
          )
        );
        setError("");
      })
      .catch((err) => {
        console.error("User orders error:", err);
        if (!cancelled) setError(err.message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [userId]);

  if (!user) {
    return (
      <div className="user-profile-empty">
        <p>No user selected.</p>
        <button onClick={onBack}>Back to Users</button>
      </div>
    );
  }

  const isAdminAccount = user.role === "ADMIN";
  const isSelf = user.id === currentUserId;

  const changeStatus = async (suspend) => {
    setConfirmingSuspend(false);
    setSaving(true);
    setActionError("");
    setNotice("");
    try {
      const updated = await setSuspended(user, suspend);
      onUserChanged?.(updated);
      setNotice(suspend ? "Account suspended." : "Account reactivated.");
    } catch (err) {
      setActionError(err.message === "Failed to fetch" ? "Cannot reach the server." : err.message);
    } finally {
      setSaving(false);
    }
  };

  const finished = (status) =>
    ["collected", "cancelled"].includes((status || "").toLowerCase());

  return (
    <div className="user-profile-page">

      {/* PAGE HEADER */}
      <div className="user-profile-header">

        <div>
          <span className="profile-breadcrumb">
            EDUEATS / USERS / DETAIL
          </span>

          <h2>User Profile Detail</h2>
        </div>

        <button
          className="profile-back-button"
          onClick={onBack}
        >
          ← Back to Users
        </button>

      </div>

      {/* MAIN CONTENT */}
      <div className="user-profile-layout">

        {/* USER INFORMATION CARD */}
        <section className="profile-card">

          <div className="profile-identity">

            <div className="profile-large-avatar">
              {user.initials}
            </div>

            <h3>{user.name}</h3>

            <span
              className={`profile-user-status ${
                user.status === "Active" ? "" : "profile-user-suspended"
              }`}
            >
              {user.status}
            </span>

          </div>

          <div className="profile-divider" />

          {/* USER DETAILS */}
          <div className="profile-details">

            <div className="profile-detail-row">
              <span>Phone</span>
              <strong>{user.phone}</strong>
            </div>

            <div className="profile-detail-row">
              <span>Account Created</span>
              <strong>{user.joined}</strong>
            </div>

            <div className="profile-detail-row">
              <span>Customer Code</span>
              <strong>{user.customerCode}</strong>
            </div>

            <div className="profile-detail-row">
              <span>Email</span>
              <strong>{user.email}</strong>
            </div>

            <div className="profile-detail-row">
              <span>Role</span>
              <strong>{user.role}</strong>
            </div>

            <div className="profile-detail-row">
              <span>Last Login</span>
              <strong>
                {user.lastLogin
                  ? new Date(user.lastLogin).toLocaleString("en-ZA", {
                      day: "2-digit",
                      month: "short",
                      hour: "2-digit",
                      minute: "2-digit",
                    })
                  : "Never"}
              </strong>
            </div>

            <div className="profile-detail-row">
              <span>Total Spent</span>
              <strong>
                {formatRand(
                  orders.reduce(
                    (sum, order) =>
                      (order.status || "").toLowerCase() === "cancelled"
                        ? sum
                        : sum + Number(order.totalPrice ?? 0),
                    0
                  )
                )}
              </strong>
            </div>

          </div>

          {/* ACTION BUTTONS */}
          {actionError && (
            <p className="profile-action-message profile-action-error" role="alert">
              {actionError}
            </p>
          )}
          {notice && (
            <p className="profile-action-message" role="status">
              {notice}
            </p>
          )}

          <div className="profile-actions">
            <button
              className="profile-edit-button"
              onClick={() => setResetOpen(true)}
              disabled={isSelf}
              title={isSelf ? "Change your own password from your profile instead" : ""}
            >
              ⚿ Reset Password
            </button>

            {isAdminAccount ? (
              <p className="profile-protected">Admin accounts can't be suspended.</p>
            ) : (
              <button
                className={`profile-suspend-button ${
                  user.status === "Active" ? "" : "profile-reactivate-button"
                }`}
                disabled={saving}
                onClick={() =>
                  user.status === "Active" ? setConfirmingSuspend(true) : changeStatus(false)
                }
              >
                ⊘ {saving ? "Saving..." : user.status === "Active" ? "Suspend" : "Reactivate"}
              </button>
            )}
          </div>

        </section>

        {/* ORDER HISTORY */}
        <section className="profile-orders-card">

          <div className="profile-orders-header">
            <h3>Order History Ledger</h3>
            <span>{orders.length} Orders</span>
          </div>

          <div className="profile-orders-table-wrapper">

            <table className="profile-orders-table">

              <thead>
                <tr>
                  <th>Order ID</th>
                  <th>Date</th>
                  <th>Items Purchased</th>
                  <th>Total</th>
                  <th>Status</th>
                </tr>
              </thead>

              <tbody>

                {orders.map((order) => (
                  <tr key={order.orderId}>

                    <td>
                      <span className="profile-order-link">
                        {order.customerOrderNumber || `#${order.orderId}`}
                      </span>
                    </td>

                    <td>{formatDate(order.createdAt)}</td>

                    <td>
                      {(order.items || [])
                        .map(
                          (item) => `${item.quantity}x ${item.menuItemName}`
                        )
                        .join(", ")}
                    </td>

                    <td>{formatRand(order.totalPrice)}</td>

                    <td>
                      <span
                        className={`profile-order-status ${
                          finished(order.status)
                            ? "order-status-delayed"
                            : "order-status-active"
                        }`}
                      >
                        {order.status}
                      </span>
                    </td>

                  </tr>
                ))}

                {orders.length === 0 && (
                  <tr>
                    <td colSpan="5">
                      {loading
                        ? "Loading orders..."
                        : error || "This user has no orders yet."}
                    </td>
                  </tr>
                )}

              </tbody>

            </table>

          </div>

        </section>

      </div>

      {confirmingSuspend && (
        <ConfirmDialog
          title={`Suspend ${user.name}?`}
          message="They won't be able to log in until you reactivate the account. Their orders stay in the system."
          confirmLabel="Suspend"
          cancelLabel="Cancel"
          danger
          onConfirm={() => changeStatus(true)}
          onCancel={() => setConfirmingSuspend(false)}
        />
      )}

      {resetOpen && (
        <ResetPasswordForm
          user={user}
          onDone={() => {
            setResetOpen(false);
            setNotice(`New password saved for ${user.name}. Give it to them privately.`);
          }}
          onClose={() => setResetOpen(false)}
        />
      )}

    </div>
  );
}

// NEW: pop-up to give a user a new password.
// The backend asks for the admin's own password first, as a safety check.
function ResetPasswordForm({ user, onDone, onClose }) {
  const [adminPassword, setAdminPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [repeat, setRepeat] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const submit = async (event) => {
    event.preventDefault();
    setError("");

    if (!adminPassword) {
      setError("Enter your own password to confirm it's you.");
      return;
    }
    // CHANGED: same password rules as the backend
    if (checkNewPassword(newPassword)) {
      setError(checkNewPassword(newPassword));
      return;
    }
    if (newPassword !== repeat) {
      setError("The two new passwords don't match.");
      return;
    }

    setSaving(true);
    try {
      await resetPassword(user, adminPassword, newPassword);
      onDone();
    } catch (err) {
      setError(err.message === "Failed to fetch" ? "Cannot reach the server." : err.message);
      setSaving(false);
    }
  };

  return (
    <div className="admins-modal-overlay" onClick={onClose}>
      <div
        className="admins-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="reset-title"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="admins-modal-header">
          <h3 id="reset-title">Reset password</h3>
          <button className="admins-modal-close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>

        <p className="admins-modal-description">
          Set a new password for <strong>{user.name}</strong> ({user.email}). Their old password
          stops working straight away.
        </p>

        <form onSubmit={submit} noValidate>
          <div className="admins-form-group">
            <label htmlFor="reset-admin">Your admin password</label>
            <input
              id="reset-admin"
              type="password"
              autoComplete="current-password"
              value={adminPassword}
              onChange={(event) => setAdminPassword(event.target.value)}
              maxLength={64}
              autoFocus
            />
          </div>

          <div className="admins-form-group">
            <label htmlFor="reset-new">New password ({PASSWORD_HINT})</label>
            <input
              id="reset-new"
              type="password"
              autoComplete="new-password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              maxLength={64}
            />
          </div>

          <div className="admins-form-group">
            <label htmlFor="reset-repeat">Type the new password again</label>
            <input
              id="reset-repeat"
              type="password"
              autoComplete="new-password"
              value={repeat}
              onChange={(event) => setRepeat(event.target.value)}
              maxLength={64}
            />
          </div>

          {error && (
            <p className="admins-form-error" role="alert">
              {error}
            </p>
          )}

          <div className="admins-modal-actions">
            <button type="button" className="admins-cancel-button" onClick={onClose} disabled={saving}>
              Cancel
            </button>
            <button type="submit" className="admins-save-button" disabled={saving}>
              {saving ? "Saving..." : "Save new password"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default UserProfile;
