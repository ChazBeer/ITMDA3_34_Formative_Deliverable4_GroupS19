// src/pages/Profile/Profile.jsx
// My Profile: see your details, change name / email / phone,
// change your password, and delete your account.
// Backend: GET / PUT / DELETE /api/users/me
// Every change needs the CURRENT password (the backend checks it).

import { useEffect, useRef, useState } from "react";
import "./Profile.css";
import { apiFetch } from "../../api";
import AppHeader from "../../components/AppHeader";
import ConfirmDialog from "../../components/ConfirmDialog";
import PasswordInput from "../../components/PasswordInput";
import PasswordMatchHint from "../../components/PasswordMatchHint";
import PasswordRules from "../../components/PasswordRules";
import { errorFromBody } from "../../apiError";
import { LIMITS, validateEmail, validateName, validatePassword } from "../../validation";

// Same rule as the backend: optional +, then 10 to 15 digits
const PHONE_PATTERN = /^\+?[0-9]{10,15}$/;

// Spaces and dashes are allowed while typing, e.g. "082 123 4567"
const cleanPhone = (value) => String(value || "").replace(/[\s-]/g, "");

const toForm = (profile) => ({
  firstName: profile?.firstName || "",
  lastName: profile?.lastName || "",
  email: profile?.email || "",
  phoneNumber: profile?.phoneNumber || "",
});

const getInitials = (profile) =>
  `${profile?.firstName?.[0] || ""}${profile?.lastName?.[0] || ""}`.toUpperCase() || "?";

const getRoleName = (profile) => {
  const role = profile?.role?.roleName || profile?.role?.name || profile?.roleName || profile?.role;
  return typeof role === "string" ? role.replace(/^ROLE_/, "") : "";
};

const formatJoined = (value) => {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleDateString("en-ZA", { day: "numeric", month: "long", year: "numeric" });
};

// Turns a backend error into one readable sentence.
// Handles { message }, { errors: {field: msg} }, { field: msg } and plain text.
async function readError(response, fallback) {
  try {
    const text = await response.text();
    if (!text) return fallback;

    try {
      const body = JSON.parse(text);
      if (typeof body === "string") return body;
      // CHANGED: validationErrors / message from the backend's error reply
      const fromBackend = errorFromBody(body);
      if (fromBackend) return fromBackend;
      if (body?.error && typeof body.error === "string" && body.error.length < 120) {
        return body.error;
      }
      const firstValue = Object.values(body || {}).find((value) => typeof value === "string");
      return firstValue || fallback;
    } catch {
      return text.length < 160 ? text : fallback;
    }
  } catch {
    return fallback;
  }
}

function Profile({
  user,
  cartCount = 0,
  cartAlert = false,
  onCartSeen,
  newOrderCount = 0,
  onNavigate,
  onLogout,
  onUserUpdated,
}) {
  const [profile, setProfile] = useState(user || null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  // ---------- details form ----------
  const [form, setForm] = useState(() => toForm(user));
  const [detailsPassword, setDetailsPassword] = useState("");
  const [detailsErrors, setDetailsErrors] = useState({});
  const [savingDetails, setSavingDetails] = useState(false);

  // ---------- password form ----------
  const [passwords, setPasswords] = useState({ current: "", next: "", confirm: "" });
  const [passwordErrors, setPasswordErrors] = useState({});
  const [savingPassword, setSavingPassword] = useState(false);

  // ---------- delete ----------
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleting, setDeleting] = useState(false);

  // { type: "success" | "error", text }
  const [notice, setNotice] = useState(null);

  // A new id each time, so the same message shown twice still animates in again
  const noticeIdRef = useRef(0);
  const showNotice = (type, text) => {
    noticeIdRef.current += 1;
    setNotice({ type, text, id: noticeIdRef.current });
  };

  useEffect(() => {
    if (!notice) return undefined;
    const timer = setTimeout(() => setNotice(null), 5000);
    return () => clearTimeout(timer);
  }, [notice]);

  // Load the latest details from the server
  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      try {
        setLoading(true);
        const response = await apiFetch("/api/users/me");

        if (response.status === 401) {
          if (onLogout) onLogout();
          return;
        }
        if (!response.ok) {
          throw new Error(`Could not load your profile (server returned ${response.status}).`);
        }

        const data = await response.json();
        if (cancelled) return;

        setProfile(data);
        setForm(toForm(data));
        setLoadError("");
      } catch (err) {
        console.error("Profile load error:", err);
        if (!cancelled) setLoadError(err.message || "Could not load your profile.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    load();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const original = toForm(profile);
  const changedFields = Object.keys(form).filter(
    (key) =>
      (key === "phoneNumber" ? cleanPhone(form[key]) : form[key].trim()) !==
      (key === "phoneNumber" ? cleanPhone(original[key]) : original[key].trim())
  );
  const hasChanges = changedFields.length > 0;

  const updateField = (field) => (event) => {
    setForm((current) => ({ ...current, [field]: event.target.value }));
    setDetailsErrors((current) => ({ ...current, [field]: "" }));
  };

  // The backend sometimes says 401 for a wrong password. apiFetch would then
  // try a token refresh, so check if we're still logged in before logging out.
  const stillLoggedIn = async () => {
    try {
      const response = await apiFetch("/api/users/me");
      return response.ok;
    } catch {
      return false;
    }
  };

  // Sends only what changed, plus the current password
  const sendUpdate = async (changes, currentPassword) => {
    const response = await apiFetch("/api/users/me", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ ...changes, currentPassword }),
    });

    if (response.ok) {
      let data = null;
      try {
        data = await response.json();
      } catch {
        // no body - fine
      }
      return { ok: true, data };
    }

    if (response.status === 401 || response.status === 403) {
      if (await stillLoggedIn()) {
        return { ok: false, wrongPassword: true, message: "Your current password is incorrect." };
      }
      if (onLogout) onLogout();
      return { ok: false, loggedOut: true };
    }

    const message = await readError(response, `Could not save (server returned ${response.status}).`);
    const wrongPassword = /password/i.test(message) && /incorrect|invalid|wrong|match/i.test(message);
    return { ok: false, wrongPassword, message };
  };

  // ---------- save details ----------

  const saveDetails = async (event) => {
    event.preventDefault();
    if (!hasChanges) return;

    const errors = {
      firstName: validateName(form.firstName, "First name"),
      lastName: validateName(form.lastName, "Last name"),
      email: validateEmail(form.email),
      phoneNumber: "",
      currentPassword: detailsPassword ? "" : "Enter your current password to save changes.",
    };

    const phone = cleanPhone(form.phoneNumber);
    if (phone && !PHONE_PATTERN.test(phone)) {
      errors.phoneNumber = "Enter 10 to 15 digits, e.g. 0821234567 or +27821234567.";
    }
    if (!phone && original.phoneNumber) {
      errors.phoneNumber = "A phone number can't be removed, only changed.";
    }

    setDetailsErrors(errors);
    if (Object.values(errors).some(Boolean)) return;

    const changes = {};
    changedFields.forEach((field) => {
      changes[field] = field === "phoneNumber" ? phone : form[field].trim();
    });

    setSavingDetails(true);
    try {
      const result = await sendUpdate(changes, detailsPassword);
      if (result.loggedOut) return;

      if (!result.ok) {
        if (result.wrongPassword) {
          setDetailsErrors({ currentPassword: result.message });
        } else {
          showNotice("error", result.message);
        }
        return;
      }

      // Your login is linked to your email, so a new email means logging in again
      if (changes.email && changes.email.toLowerCase() !== original.email.toLowerCase()) {
        if (onLogout) onLogout("Email changed. Please log in with your new email address.");
        return;
      }

      const updated = { ...profile, ...changes, ...(result.data || {}) };
      setProfile(updated);
      setForm(toForm(updated));
      setDetailsPassword("");
      if (onUserUpdated) onUserUpdated(updated);
      showNotice("success", "Your details were saved.");
    } catch (err) {
      console.error("Profile save error:", err);
      showNotice("error", "Could not save your details. Check your connection and try again.");
    } finally {
      setSavingDetails(false);
    }
  };

  const resetDetails = () => {
    setForm(toForm(profile));
    setDetailsPassword("");
    setDetailsErrors({});
  };

  // ---------- change password ----------

  const updatePassword = (field) => (event) => {
    setPasswords((current) => ({ ...current, [field]: event.target.value }));
    setPasswordErrors((current) => ({ ...current, [field]: "" }));
  };

  const savePassword = async (event) => {
    event.preventDefault();

    const errors = {
      current: passwords.current ? "" : "Enter your current password.",
      next: validatePassword(passwords.next),
      confirm: "",
    };
    if (!errors.next && passwords.next === passwords.current) {
      errors.next = "Your new password must be different from the current one.";
    }
    if (passwords.confirm !== passwords.next) {
      errors.confirm = "The passwords don't match.";
    }

    setPasswordErrors(errors);
    if (Object.values(errors).some(Boolean)) return;

    setSavingPassword(true);
    try {
      const result = await sendUpdate({ newPassword: passwords.next }, passwords.current);
      if (result.loggedOut) return;

      if (!result.ok) {
        if (result.wrongPassword) {
          setPasswordErrors({ current: result.message });
        } else {
          showNotice("error", result.message);
        }
        return;
      }

      setPasswords({ current: "", next: "", confirm: "" });
      showNotice("success", "Your password was changed. Use the new one next time you log in.");
    } catch (err) {
      console.error("Password change error:", err);
      showNotice("error", "Could not change your password. Check your connection and try again.");
    } finally {
      setSavingPassword(false);
    }
  };

  // ---------- delete account ----------

  const deleteAccount = async () => {
    setConfirmDelete(false);
    setDeleting(true);

    try {
      const response = await apiFetch("/api/users/me", { method: "DELETE" });

      if (!response.ok && response.status !== 401) {
        const message = await readError(response, "Could not delete your account.");
        showNotice("error", message);
        return;
      }

      if (onLogout) onLogout("Your account has been deleted. Sorry to see you go!");
    } catch (err) {
      console.error("Delete account error:", err);
      showNotice("error", "Could not delete your account. Check your connection and try again.");
    } finally {
      setDeleting(false);
    }
  };

  const roleName = getRoleName(profile);
  const joined = formatJoined(profile?.createdAt);
  const fullName = [profile?.firstName, profile?.lastName].filter(Boolean).join(" ");

  return (
    <div className="profile-page">
      <AppHeader
        user={profile || user}
        current="profile"
        cartCount={cartCount}
        cartAlert={cartAlert}
        onCartSeen={onCartSeen}
        newOrderCount={newOrderCount}
        onNavigate={onNavigate}
        onLogout={onLogout}
      />

      {notice && (
        <div className={`profile-notice notice-${notice.type}`} role="status" key={notice.id}>
          <span>{notice.text}</span>
          <button type="button" onClick={() => setNotice(null)} aria-label="Close message">
            ×
          </button>
        </div>
      )}

      <main className="profile-content page-enter">
        <h2 className="profile-title">My Profile</h2>

        {/* ---------- summary card ---------- */}
        <section className="profile-card profile-summary">
          <div className="profile-avatar" aria-hidden="true">
            {getInitials(profile)}
          </div>
          <div className="profile-summary-text">
            <h3>{fullName || "Your account"}</h3>
            <p>{profile?.email}</p>
            {profile?.phoneNumber && <p>{profile.phoneNumber}</p>}
            <div className="profile-tags">
              {roleName && <span className="profile-tag">{roleName}</span>}
              {joined && <span className="profile-joined">Member since {joined}</span>}
            </div>
          </div>
        </section>

        {loading && <p className="profile-loading">Loading your latest details…</p>}
        {!loading && loadError && <p className="profile-load-error">{loadError}</p>}

        {/* ---------- edit details ---------- */}
        <form className="profile-card" onSubmit={saveDetails} noValidate>
          <h3>Your details</h3>
          <p className="profile-card-hint">Change what you need, then confirm with your password.</p>

          <div className="profile-grid">
            <Field label="First name" id="firstName" error={detailsErrors.firstName}>
              <input
                id="firstName"
                value={form.firstName}
                maxLength={LIMITS.nameMax}
                autoComplete="given-name"
                onChange={updateField("firstName")}
              />
            </Field>

            <Field label="Last name" id="lastName" error={detailsErrors.lastName}>
              <input
                id="lastName"
                value={form.lastName}
                maxLength={LIMITS.nameMax}
                autoComplete="family-name"
                onChange={updateField("lastName")}
              />
            </Field>

            <Field
              label="Email address"
              id="email"
              error={detailsErrors.email}
              hint={
                form.email.trim().toLowerCase() !== original.email.trim().toLowerCase()
                  ? "You'll be asked to log in again with your new email."
                  : ""
              }
            >
              <input
                id="email"
                type="email"
                value={form.email}
                maxLength={LIMITS.emailMax}
                autoComplete="email"
                onChange={updateField("email")}
              />
            </Field>

            <Field label="Phone number" id="phoneNumber" optional error={detailsErrors.phoneNumber}>
              <input
                id="phoneNumber"
                type="tel"
                inputMode="tel"
                placeholder="e.g. 0821234567"
                value={form.phoneNumber}
                maxLength={20}
                autoComplete="tel"
                onChange={updateField("phoneNumber")}
              />
            </Field>
          </div>

          {hasChanges && (
            <div className="profile-confirm-password">
              <Field label="Current password" id="detailsPassword" error={detailsErrors.currentPassword}>
                <PasswordInput
                  id="detailsPassword"
                  placeholder="Needed to save your changes"
                  value={detailsPassword}
                  maxLength={LIMITS.passwordMax}
                  onChange={(event) => {
                    setDetailsPassword(event.target.value);
                    setDetailsErrors((current) => ({ ...current, currentPassword: "" }));
                  }}
                />
              </Field>
            </div>
          )}

          <div className="profile-actions">
            {hasChanges && (
              <button type="button" className="profile-button secondary" onClick={resetDetails}>
                Undo changes
              </button>
            )}
            <button
              type="submit"
              className="profile-button"
              disabled={!hasChanges || savingDetails}
            >
              {savingDetails ? "Saving…" : "Save changes"}
            </button>
          </div>
        </form>

        {/* ---------- change password ---------- */}
        <form className="profile-card" onSubmit={savePassword} noValidate>
          <h3>Change password</h3>
          <p className="profile-card-hint">
            At least {LIMITS.passwordMin} characters, with a capital letter, a number and a
            symbol. You'll stay logged in.
          </p>

          <Field label="Current password" id="currentPassword" error={passwordErrors.current}>
            <PasswordInput
              id="currentPassword"
              placeholder="Your current password"
              value={passwords.current}
              maxLength={LIMITS.passwordMax}
              onChange={updatePassword("current")}
            />
          </Field>

          <div className="profile-grid">
            <Field label="New password" id="newPassword" error={passwordErrors.next}>
              <PasswordInput
                id="newPassword"
                placeholder="New password"
                value={passwords.next}
                maxLength={LIMITS.passwordMax}
                onChange={updatePassword("next")}
              />
              <PasswordRules password={passwords.next} />
            </Field>

            <Field label="Confirm new password" id="confirmPassword" error={passwordErrors.confirm}>
              <PasswordInput
                id="confirmPassword"
                placeholder="Type it again"
                value={passwords.confirm}
                maxLength={LIMITS.passwordMax}
                onChange={updatePassword("confirm")}
              />
              {!passwordErrors.confirm && (
                <PasswordMatchHint password={passwords.next} confirm={passwords.confirm} />
              )}
            </Field>
          </div>

          <div className="profile-actions">
            <button
              type="submit"
              className="profile-button"
              disabled={savingPassword || !passwords.current || !passwords.next}
            >
              {savingPassword ? "Changing…" : "Change password"}
            </button>
          </div>
        </form>

        {/* ---------- delete account ---------- */}
        <section className="profile-card profile-danger">
          <div>
            <h3>Delete account</h3>
            <p className="profile-card-hint">
              Your account will be closed and you'll be logged out. This can't be undone.
            </p>
          </div>
          <button
            type="button"
            className="profile-button danger"
            onClick={() => setConfirmDelete(true)}
            disabled={deleting}
          >
            {deleting ? "Deleting…" : "Delete my account"}
          </button>
        </section>
      </main>

      {confirmDelete && (
        <ConfirmDialog
          danger
          title="Delete your account?"
          message="Your account will be closed and you'll be logged out straight away. This can't be undone."
          confirmLabel="Yes, delete"
          cancelLabel="Keep account"
          onConfirm={deleteAccount}
          onCancel={() => setConfirmDelete(false)}
        />
      )}
    </div>
  );
}

// Label + input + error message, laid out the same everywhere
function Field({ label, id, optional = false, hint = "", error = "", children }) {
  return (
    <div className={`profile-field ${error ? "has-error" : ""}`}>
      <label htmlFor={id}>
        {label}
        {optional && <span className="profile-optional"> (optional)</span>}
      </label>
      {children}
      {error ? (
        <p className="profile-error" role="alert">
          {error}
        </p>
      ) : (
        hint && <p className="profile-hint">{hint}</p>
      )}
    </div>
  );
}

export default Profile;
