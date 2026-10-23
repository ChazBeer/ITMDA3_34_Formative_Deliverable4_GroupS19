import { useState } from "react";
import "./Login.css";
import { API_BASE_URL } from "../api";
import PasswordInput from "./PasswordInput";

// Only these roles may use the staff dashboard
const STAFF_ROLES = ["STAFF", "ADMIN"];

// The profile from /api/auth/me may name the role in different ways,
// so check the common ones
function getRoleName(user) {
  const role =
    user?.role?.roleName ||
    user?.role?.name ||
    user?.roleName ||
    user?.role ||
    "";

  return String(role).toUpperCase();
}

function Login({ onLogin }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showForgotHelp, setShowForgotHelp] = useState(false); // NEW
  const [rememberTerminal, setRememberTerminal] = useState(true);
  const [error, setError] = useState("");

  // NEW: true while waiting for the backend
  const [loading, setLoading] = useState(false);

  // CHANGED: real login against the backend (was a demo login)
  const handleSubmit = async (event) => {
    event.preventDefault();

    // Basic frontend validation
    if (!email || !password) {
      setError("Please enter your email and password.");
      return;
    }

    setError("");
    setLoading(true);

    try {
      // 1. Log in
      // Staff have their own login (and their own refresh cookie),
      // so the student app and staff app never share a session
      const loginResponse = await fetch(`${API_BASE_URL}/api/auth/staff-login`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: email.trim(), password }),
      });

      // NEW: suspended accounts get a clear message
      if (!loginResponse.ok) {
        let reason = "";
        try {
          const data = await loginResponse.clone().json();
          reason = `${data?.message || ""} ${data?.error || ""}`;
        } catch {
          // no JSON in the reply
        }
        if (/suspend|inactive|disabled/i.test(reason)) {
          throw new Error("This account has been suspended. Please speak to an admin.");
        }
      }

      // 403 = correct password, but a student/customer account
      if (loginResponse.status === 403) {
        throw new Error(
          "This account does not have staff access. Please use the student app."
        );
      }

      if (!loginResponse.ok) {
        throw new Error("Invalid email or password.");
      }

      const loginData = await loginResponse.json();
      const token = loginData.token;

      if (!token) {
        throw new Error("No login token was returned.");
      }

      // 2. Get the logged-in user's profile
      const profileResponse = await fetch(`${API_BASE_URL}/api/auth/me`, {
        credentials: "include",
        headers: { Authorization: `Bearer ${token}` },
      });

      if (!profileResponse.ok) {
        throw new Error("Could not load your profile.");
      }

      const user = await profileResponse.json();

      // 3. Only staff and admins may use this app
      const role = getRoleName(user);

      if (role && !STAFF_ROLES.includes(role)) {
        throw new Error(
          "This account does not have staff access. Please use the student app."
        );
      }

      // 4. Save the session
      localStorage.setItem("token", token);
      localStorage.setItem("staffUser", JSON.stringify(user));

      // "Remember Terminal": stay logged in after the browser is closed.
      // If unticked, App.jsx logs out once the tab has been closed.
      localStorage.setItem("staffRemember", rememberTerminal ? "true" : "false");
      sessionStorage.setItem("staffSessionActive", "true");

      onLogin(user, token);
    } catch (err) {
      console.error("Staff login error:", err);
      localStorage.removeItem("token");
      localStorage.removeItem("staffUser");
      setError(err.message || "Login failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">

        {/* LOGO */}
        <div className="login-logo">
          E
        </div>

        {/* HEADING */}
        <div className="login-heading">
          <h1>Staff Sign In</h1>
          <p>Enter your corporate credentials to continue.</p>
        </div>

        {/* LOGIN FORM */}
        <form onSubmit={handleSubmit}>

          {/* EMAIL */}
          <div className="login-field">
            <label htmlFor="email">
              Work Email Address
            </label>

            <input
              id="email"
              type="email"
              placeholder="s.jenkins@edueats.com"
              value={email}
              maxLength={100}
              autoComplete="email"
              onChange={(event) => setEmail(event.target.value)}
            />
          </div>

          {/* PASSWORD */}
          <div className="login-field">
            <label htmlFor="password">
              Security Password
            </label>

            {/* NEW: Show / Hide button, same as the student app */}
            <PasswordInput
              id="password"
              placeholder="Enter your password"
              value={password}
              maxLength={64}
              onChange={(event) => setPassword(event.target.value)}
            />
          </div>

          {/* REMEMBER TERMINAL */}
          <div className="login-options">
            <label className="remember-option">
              <input
                type="checkbox"
                checked={rememberTerminal}
                onChange={(event) =>
                  setRememberTerminal(event.target.checked)
                }
              />

              <span>Remember Terminal</span>
            </label>

            <button
              type="button"
              className="forgot-password"
              onClick={() => setShowForgotHelp((current) => !current)}
              aria-expanded={showForgotHelp}
            >
              Forgot password?
            </button>
          </div>

          {/* NEW: shown instead of a browser pop-up */}
          {showForgotHelp && (
            <p className="login-help" role="status">
              Ask an EduEats admin to reset it. Admins can set a new password in Admin Panel ›
              Users, Staff or Admins › View › Reset Password.
            </p>
          )}

          {/* ERROR MESSAGE */}
          {error && (
            <div className="login-error">
              {error}
            </div>
          )}

          {/* LOGIN BUTTON */}
          <button
            type="submit"
            className="login-submit"
            disabled={loading}
          >
            {loading ? "Signing In..." : "Access Dashboard"}
          </button>

        </form>

        {/* FOOTER */}
        <div className="login-footer">
          Trouble logging in?

          <button
            type="button"
            onClick={() => setShowForgotHelp(true)}
          >
            Contact IT Support
          </button>
        </div>

      </div>
    </div>
  );
}

export default Login;
