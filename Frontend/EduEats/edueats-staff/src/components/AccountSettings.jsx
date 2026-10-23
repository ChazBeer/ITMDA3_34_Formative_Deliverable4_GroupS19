// src/components/AccountSettings.jsx
// Settings page for staff and admins:
// - edit your own name, email and phone (PUT /api/users/me)
// - change your password (same request, with the new password)
// - new-order sound on/off
// - deactivate your own account (DELETE /api/users/me)
// Every change needs the current password (the backend checks it).

import { useState } from "react";
import "./AccountSettings.css";
import { apiFetch } from "../api";
import { errorFromBody } from "../apiError";
import PasswordInput from "./PasswordInput";
import ConfirmDialog from "./ConfirmDialog";
import { checkNewPassword, PASSWORD_HINT } from "../passwordRules";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_PATTERN = /^\+?[0-9]{10,15}$/;
const cleanPhone = (value) => String(value || "").replace(/[\s-]/g, "");

const detailsOf = (user) => ({
  firstName: user?.firstName || "",
  lastName: user?.lastName || "",
  email: user?.email || "",
  phoneNumber: user?.phoneNumber || "",
});

// The backend's error text, or a friendly fallback
async function readError(response, fallback) {
  if (response.status === 401) return "Your session has ended. Please log in again.";
  try {
    const data = await response.json();
    const text = errorFromBody(data);
    if (/current password/i.test(text || "")) return "Your current password is not correct.";
    if (text && !/unexpected error/i.test(text)) return text;
  } catch {
    // no JSON in the reply
  }
  return fallback;
}

function AccountSettings({ staffUser, roleName, soundOn, onSoundChange, onBack, onUserUpdated, onLogout }) {
  const original = detailsOf(staffUser);
  const [form, setForm] = useState(original);
  const [newPassword, setNewPassword] = useState("");
  const [repeatPassword, setRepeatPassword] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [confirmDeactivate, setConfirmDeactivate] = useState(false);
  const [deactivating, setDeactivating] = useState(false);

  const update = (field) => (event) => {
    setForm((current) => ({ ...current, [field]: event.target.value }));
    setNotice("");
  };

  const changedFields = Object.keys(form).filter((field) =>
    field === "phoneNumber"
      ? cleanPhone(form[field]) !== cleanPhone(original[field])
      : form[field].trim() !== original[field].trim()
  );
  const hasChanges = changedFields.length > 0 || newPassword.length > 0;

  const save = async (event) => {
    event.preventDefault();
    setError("");
    setNotice("");

    // Checked here first, because the backend gives a general error for these
    if (!form.firstName.trim() || !form.lastName.trim()) {
      return setError("Please enter your first name and surname.");
    }
    if (!EMAIL_PATTERN.test(form.email.trim())) {
      return setError("Please enter a valid email address.");
    }
    const phone = cleanPhone(form.phoneNumber);
    if (phone && !PHONE_PATTERN.test(phone)) {
      return setError("Enter a phone number with 10 to 15 digits, e.g. 0821234567.");
    }
    if (!phone && original.phoneNumber) {
      return setError("A phone number can't be removed, only changed.");
    }
    // CHANGED: same password rules as the backend
    if (newPassword && checkNewPassword(newPassword)) {
      return setError(checkNewPassword(newPassword));
    }
    if (newPassword && newPassword !== repeatPassword) {
      return setError("The two new passwords don't match.");
    }
    if (!hasChanges) {
      return setError("Nothing has changed yet.");
    }
    if (currentPassword.length < 8) {
      return setError("Enter your current password to save the changes.");
    }

    // Only send what changed (empty fields would fail the backend's checks)
    const body = { currentPassword };
    changedFields.forEach((field) => {
      body[field] = field === "phoneNumber" ? phone : form[field].trim();
    });
    if (newPassword) body.newPassword = newPassword;

    setSaving(true);
    try {
      const response = await apiFetch("/api/users/me", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      if (!response.ok) {
        throw new Error(await readError(response, "Your changes could not be saved."));
      }
      const saved = await response.json().catch(() => ({}));

      setCurrentPassword("");
      setNewPassword("");
      setRepeatPassword("");

      // A new email means a new login (the login token belongs to the old email)
      if (body.email) {
        setNotice("Saved. Your email changed, so please log in again with your new email.");
        setTimeout(onLogout, 2500);
        return;
      }

      onUserUpdated({
        firstName: saved.firstName ?? body.firstName ?? staffUser?.firstName,
        lastName: saved.lastName ?? body.lastName ?? staffUser?.lastName,
        phoneNumber: saved.phoneNumber ?? body.phoneNumber ?? staffUser?.phoneNumber,
      });
      setNotice(newPassword ? "Your details and new password are saved." : "Your details are saved.");
    } catch (err) {
      setError(err.message === "Failed to fetch" ? "Cannot reach the server." : err.message);
    } finally {
      setSaving(false);
    }
  };

  const deactivate = async () => {
    setConfirmDeactivate(false);
    setDeactivating(true);
    setError("");
    try {
      const response = await apiFetch("/api/users/me", { method: "DELETE" });
      if (!response.ok) {
        throw new Error(await readError(response, "Your account could not be deactivated."));
      }
      onLogout();
    } catch (err) {
      setError(err.message === "Failed to fetch" ? "Cannot reach the server." : err.message);
      setDeactivating(false);
    }
  };

  const isAdmin = roleName === "ADMIN";

  return (
    <main className="settings-page">
      <div className="settings-header">
        <div>
          <h1>Account Settings</h1>
          <p>Update your details, password and preferences.</p>
        </div>
        <button className="secondary-button" onClick={onBack}>
          ← Back to Orders
        </button>
      </div>

      {error && (
        <div className="account-message account-error" role="alert">
          {error}
        </div>
      )}
      {notice && (
        <div className="account-message" role="status">
          {notice}
        </div>
      )}

      <form className="settings-card account-card" onSubmit={save} noValidate>
        <div className="settings-card-title">
          <h2>My details</h2>
          <span className="account-role">{roleName || "STAFF"}</span>
        </div>

        <div className="account-grid">
          <label>
            First name
            <input value={form.firstName} onChange={update("firstName")} maxLength={60} autoComplete="given-name" />
          </label>
          <label>
            Surname
            <input value={form.lastName} onChange={update("lastName")} maxLength={60} autoComplete="family-name" />
          </label>
          <label>
            Email
            <input type="email" value={form.email} onChange={update("email")} maxLength={120} autoComplete="email" />
          </label>
          <label>
            <span>
              Phone <small>(optional)</small>
            </span>
            <input
              type="tel"
              value={form.phoneNumber}
              onChange={update("phoneNumber")}
              maxLength={20}
              autoComplete="tel"
              placeholder="e.g. 0821234567"
            />
          </label>
        </div>

        <div className="settings-card-title account-subtitle">
          <h2>Change password</h2>
          <span>Leave empty to keep your password</span>
        </div>

        <div className="account-grid">
          <label>
            <span>
              New password <small>({PASSWORD_HINT})</small>
            </span>
            <PasswordInput
              id="new-password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              maxLength={64}
              autoComplete="new-password"
            />
          </label>
          <label>
            Type the new password again
            <PasswordInput
              id="repeat-password"
              value={repeatPassword}
              onChange={(event) => setRepeatPassword(event.target.value)}
              maxLength={64}
              autoComplete="new-password"
            />
          </label>
        </div>

        <div className="account-save">
          <label>
            <span>
              Current password <small>(needed to save any change)</small>
            </span>
            <PasswordInput
              id="current-password"
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
              maxLength={64}
            />
          </label>
          <button type="submit" className="primary-button" disabled={saving || !hasChanges}>
            {saving ? "Saving..." : "Save changes"}
          </button>
        </div>
      </form>

      <section className="settings-card account-card">
        <div className="settings-card-title">
          <h2>Preferences</h2>
        </div>
        <div className="account-row">
          <div>
            <strong>New-order sound</strong>
            <p>Plays a short sound when a new order arrives on the board.</p>
          </div>
          <label className="settings-toggle">
            <input
              type="checkbox"
              checked={soundOn}
              onChange={(event) => onSoundChange(event.target.checked)}
            />
            {soundOn ? "On" : "Off"}
          </label>
        </div>
      </section>

      <section className="settings-card account-card account-danger">
        <div className="settings-card-title">
          <h2>Deactivate account</h2>
        </div>
        <div className="account-row">
          <div>
            <strong>Switch off your account</strong>
            <p>
              You are logged out and can't log in again until an admin reactivates you in the Admin
              Panel. Orders you handled stay in the system.
              {isAdmin && " Make sure another admin account exists first."}
            </p>
          </div>
          <button
            type="button"
            className="account-danger-button"
            onClick={() => setConfirmDeactivate(true)}
            disabled={deactivating}
          >
            {deactivating ? "Deactivating..." : "Deactivate account"}
          </button>
        </div>
      </section>

      {confirmDeactivate && (
        <ConfirmDialog
          title="Deactivate your account?"
          message="You will be logged out straight away and won't be able to log in until an admin reactivates your account."
          confirmLabel="Deactivate"
          cancelLabel="Cancel"
          danger
          onConfirm={deactivate}
          onCancel={() => setConfirmDeactivate(false)}
        />
      )}
    </main>
  );
}

export default AccountSettings;
