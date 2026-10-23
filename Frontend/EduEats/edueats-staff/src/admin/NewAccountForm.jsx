import { useState } from "react";
import "./AdminsPage.css";
import { createAccount } from "./adminData";
import { checkNewPassword, PASSWORD_HINT } from "../passwordRules";

// NEW: pop-up to create a staff or admin account (POST /api/users?roleId=...).
// roleName is "STAFF" or "ADMIN"; the id is looked up from the database.
// The new person logs in on the staff app with the temporary password.
const EMPTY = { firstName: "", lastName: "", email: "", phoneNumber: "", password: "" };
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function NewAccountForm({ roleName, roleLabel, onCreated, onClose }) {
  const [form, setForm] = useState(EMPTY);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const update = (event) => {
    const { name, value } = event.target;
    setForm((previous) => ({ ...previous, [name]: value }));
  };

  const submit = async (event) => {
    event.preventDefault();
    setError("");

    // Checked here because the backend gives a general error for these
    if (!form.firstName.trim() || !form.lastName.trim()) {
      setError("Please enter a first name and surname.");
      return;
    }
    if (!EMAIL_PATTERN.test(form.email.trim())) {
      setError("Please enter a valid email address.");
      return;
    }
    // CHANGED: same password rules as the backend
    if (checkNewPassword(form.password)) {
      setError(checkNewPassword(form.password));
      return;
    }

    setSaving(true);
    try {
      const account = await createAccount(form, roleName);
      onCreated(account);
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
        aria-labelledby="new-account-title"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="admins-modal-header">
          <h3 id="new-account-title">New {roleLabel} Account</h3>
          <button className="admins-modal-close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>

        <p className="admins-modal-description">
          They log in on the staff app with this email and the temporary password. Ask them to
          keep the password private.
        </p>

        <form onSubmit={submit} noValidate>
          <div className="admins-form-row">
            <div className="admins-form-group">
              <label htmlFor="new-first">First name</label>
              <input id="new-first" name="firstName" value={form.firstName} onChange={update} maxLength={60} autoFocus />
            </div>
            <div className="admins-form-group">
              <label htmlFor="new-last">Surname</label>
              <input id="new-last" name="lastName" value={form.lastName} onChange={update} maxLength={60} />
            </div>
          </div>

          <div className="admins-form-group">
            <label htmlFor="new-email">Email address</label>
            <input id="new-email" name="email" type="email" value={form.email} onChange={update} maxLength={120} />
          </div>

          <div className="admins-form-group">
            <label htmlFor="new-phone">Phone (optional)</label>
            <input id="new-phone" name="phoneNumber" type="tel" value={form.phoneNumber} onChange={update} maxLength={20} />
          </div>

          <div className="admins-form-group">
            <label htmlFor="new-password">Temporary password ({PASSWORD_HINT})</label>
            <input
              id="new-password"
              name="password"
              type="password"
              autoComplete="new-password"
              value={form.password}
              onChange={update}
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
              {saving ? "Creating..." : `Create ${roleLabel}`}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default NewAccountForm;
