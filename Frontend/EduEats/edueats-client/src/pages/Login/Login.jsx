import { useState } from "react";
import "./Login.css";
import {
  API_BASE_URL,
  SERVER_ERROR_MESSAGE,
  SERVER_UNREACHABLE_MESSAGE,
  isServerDownStatus,
} from "../../api";
import PasswordInput from "../../components/PasswordInput";
import { LIMITS, validateEmail } from "../../validation";

// NEW: shown when an admin has suspended the account
const SUSPENDED_MESSAGE =
  "Your account has been suspended. Please speak to the cafeteria or campus admin.";

// NEW: the backend's error text ({ message } or { error }), or "" if none
async function readErrorMessage(response) {
  try {
    const data = await response.clone().json();
    return `${data?.message || ""} ${data?.error || ""}`.trim();
  } catch {
    return "";
  }
}

function Login({ onGoToRegister, onLoginSuccess }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  // NEW: shows the "contact admin" note when "Forgot password?" is clicked
  const [showForgotHelp, setShowForgotHelp] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();

    setError("");

    // NEW: check the email before sending anything to the backend
    const emailError = validateEmail(email);

    if (emailError) {
      setError(emailError);
      return;
    }

    if (password.length > LIMITS.passwordMax) {
      setError(`Password can be at most ${LIMITS.passwordMax} characters.`);
      return;
    }

    setLoading(true);

    try {
      // 1. Log the user in
      // CHANGED: credentials: "include" lets the browser save the
      // refresh-token cookie, which keeps the student logged in
      let loginResponse;

      try {
        loginResponse = await fetch(`${API_BASE_URL}/api/auth/login`, {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            email: email.trim(),
            password,
          }),
        });
      } catch {
        // No answer at all: no internet, or the server is off
        throw new Error(SERVER_UNREACHABLE_MESSAGE);
      }

      // CHANGED: only say "invalid email or password" when that's really the problem
      if (isServerDownStatus(loginResponse.status)) {
        throw new Error(SERVER_UNREACHABLE_MESSAGE);
      }

      if (loginResponse.status >= 500) {
        throw new Error(SERVER_ERROR_MESSAGE);
      }

      // NEW: suspended accounts get a clear message (backend sends 400 or 403
      // with "User account has been suspended.")
      if (!loginResponse.ok) {
        const reason = await readErrorMessage(loginResponse);

        if (/suspend|inactive|disabled/i.test(reason)) {
          throw new Error(SUSPENDED_MESSAGE);
        }
      }

      // 403 = correct password, but a staff/admin account (they use the staff app)
      if (loginResponse.status === 403) {
        throw new Error("This is a staff account. Please sign in on the staff app instead.");
      }

      if (!loginResponse.ok) {
        throw new Error("Invalid email or password.");
      }

      const loginData = await loginResponse.json();
      const token = loginData.token;

      if (!token) {
        throw new Error("No authentication token was returned.");
      }

      // 2. Store JWT token
      localStorage.setItem("token", token);

      // 3. Retrieve the logged-in user's profile
      let profileResponse;

      try {
        profileResponse = await fetch(`${API_BASE_URL}/api/auth/me`, {
          method: "GET",
          credentials: "include",
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });
      } catch {
        throw new Error(SERVER_UNREACHABLE_MESSAGE);
      }

      if (isServerDownStatus(profileResponse.status)) {
        throw new Error(SERVER_UNREACHABLE_MESSAGE);
      }

      if (!profileResponse.ok) {
        throw new Error("Could not retrieve your user profile.");
      }

      const user = await profileResponse.json();

      // 4. Store user information
      localStorage.setItem("user", JSON.stringify(user));

      // 5. Tell App.jsx that login succeeded
      if (onLoginSuccess) {
        onLoginSuccess(user, token);
      }
    } catch (error) {
      console.error("Login error:", error);

      localStorage.removeItem("token");
      localStorage.removeItem("user");

      setError(error.message || "Login failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        {/* CHANGED: brand row with "E" logo tile, matching the staff app */}
        <div className="auth-brand">
          <div className="auth-logo">E</div>

          <div className="auth-brand-text">
            <strong>EduEats</strong>
            <span>Campus Cafeteria Ordering</span>
          </div>
        </div>

        <h2 className="auth-title">Welcome Back</h2>

        <p className="auth-subtitle">
          Sign in to order your favourite meals.
        </p>

        {/* CHANGED: error message uses a CSS class instead of inline styles */}
        {error && <p className="auth-error">{error}</p>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="email">Email Address</label>

            <input
              id="email"
              type="email"
              placeholder="Enter your email"
              value={email}
              maxLength={LIMITS.emailMax}
              autoComplete="email"
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <div className="label-row">
              <label htmlFor="password">Password</label>

              {/* NEW: forgot password link */}
              <a
                href="#"
                className="forgot-link"
                onClick={(event) => {
                  event.preventDefault();
                  setShowForgotHelp((current) => !current);
                }}
              >
                Forgot password?
              </a>
            </div>

            {/* NEW: our own Show / Hide button */}
            <PasswordInput
              id="password"
              placeholder="Enter your password"
              value={password}
              maxLength={LIMITS.passwordMax}
              onChange={(event) => setPassword(event.target.value)}
            />
          </div>

          {showForgotHelp && (
            <p className="auth-info">
              Please contact the cafeteria admin to reset your password.
            </p>
          )}

          <button
            type="submit"
            className="auth-button"
            disabled={loading}
          >
            {loading ? "Signing In..." : "Sign In"}
          </button>
        </form>

        <p className="auth-switch">
          Don't have an account?{" "}
          <a
            href="#"
            onClick={(event) => {
              event.preventDefault();

              if (onGoToRegister) {
                onGoToRegister();
              }
            }}
          >
            Create an account
          </a>
        </p>
      </div>
    </div>
  );
}

export default Login;
