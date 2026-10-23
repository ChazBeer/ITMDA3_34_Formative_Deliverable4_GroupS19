import { useState } from "react";
import "./Register.css";
import {
  API_BASE_URL,
  SERVER_ERROR_MESSAGE,
  SERVER_UNREACHABLE_MESSAGE,
  isServerDownStatus,
} from "../../api";
import PasswordInput from "../../components/PasswordInput";
import PasswordMatchHint from "../../components/PasswordMatchHint";
import PasswordRules from "../../components/PasswordRules";
import { errorFromBody } from "../../apiError";
import {
  LIMITS,
  validateName,
  validateEmail,
  validatePassword,
} from "../../validation";

const EMPTY_FORM = {
  firstName: "",
  lastName: "",
  email: "",
  password: "",
  confirmPassword: "",
};

// NEW: checks every field and returns { fieldName: "error message" }
function validateForm(formData) {
  const errors = {
    firstName: validateName(formData.firstName, "First name"),
    lastName: validateName(formData.lastName, "Last name"),
    email: validateEmail(formData.email),
    password: validatePassword(formData.password),
    confirmPassword: "",
  };

  if (!formData.confirmPassword) {
    errors.confirmPassword = "Please confirm your password.";
  } else if (formData.password !== formData.confirmPassword) {
    errors.confirmPassword = "Passwords do not match.";
  }

  // Keep only the fields that actually have an error
  return Object.fromEntries(
    Object.entries(errors).filter(([, message]) => message)
  );
}

function Register({ onGoToLogin }) {
  const [formData, setFormData] = useState(EMPTY_FORM);

  const [isSubmitting, setIsSubmitting] = useState(false);

  // NEW: error messages shown under each field
  const [fieldErrors, setFieldErrors] = useState({});

  // NEW: error from the backend (e.g. "Email is already registered!")
  const [serverError, setServerError] = useState("");

  const handleChange = (event) => {
    const { name, value } = event.target;

    setFormData((previousData) => ({
      ...previousData,
      [name]: value,
    }));

    // Clear a field's error as soon as the user starts fixing it
    if (fieldErrors[name]) {
      setFieldErrors((previousErrors) => {
        const updated = { ...previousErrors };
        delete updated[name];
        return updated;
      });
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    setServerError("");

    // NEW: check the form before sending it to the backend
    const errors = validateForm(formData);
    setFieldErrors(errors);

    if (Object.keys(errors).length > 0) {
      return;
    }

    const registrationData = {
      firstName: formData.firstName.trim(),
      lastName: formData.lastName.trim(),
      email: formData.email.trim(),
      password: formData.password,
    };

    setIsSubmitting(true);

    try {
      let response;

      try {
        response = await fetch(`${API_BASE_URL}/api/auth/register`, {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(registrationData),
        });
      } catch {
        // No answer at all: no internet, or the server is off
        throw new Error(SERVER_UNREACHABLE_MESSAGE);
      }

      if (isServerDownStatus(response.status)) {
        throw new Error(SERVER_UNREACHABLE_MESSAGE);
      }

      let data = null;

      try {
        data = await response.json();
      } catch {
        data = null;
      }

      if (!response.ok) {
        // CHANGED: shows the backend's field errors (validationErrors) too
        const errorMessage =
          errorFromBody(data) ||
          (response.status >= 500
            ? SERVER_ERROR_MESSAGE
            : `Registration failed (server returned ${response.status}).`);

        throw new Error(errorMessage);
      }

      setFormData(EMPTY_FORM);

      // CHANGED: no browser pop-up; the login page shows this message instead
      if (onGoToLogin) {
        onGoToLogin("Account created! You can now sign in.");
      }
    } catch (error) {
      console.error("Registration error:", error);

      // CHANGED: shown in the form instead of a popup
      setServerError(
        error.message || "Unable to create account. Please try again."
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  // Small helper so each field shows its own error underneath
  const renderError = (field) =>
    fieldErrors[field] && (
      <p className="field-error">{fieldErrors[field]}</p>
    );

  return (
    <div className="register-page">
      <div className="register-card">
        {/* Brand row with "E" logo tile, matching the staff app */}
        <div className="auth-brand">
          <div className="auth-logo">E</div>

          <div className="auth-brand-text">
            <strong>EduEats</strong>
            <span>Campus Cafeteria Ordering</span>
          </div>
        </div>

        <h2 className="auth-title">Create Account</h2>

        <p className="auth-subtitle">
          Register to start ordering your favourite meals.
        </p>

        {serverError && <p className="auth-error">{serverError}</p>}

        <form onSubmit={handleSubmit} noValidate>
          <div className="name-fields">
            <div className="form-group">
              <label htmlFor="firstName">First Name</label>

              <input
                id="firstName"
                name="firstName"
                type="text"
                placeholder="Enter your first name"
                value={formData.firstName}
                maxLength={LIMITS.nameMax}
                autoComplete="given-name"
                onChange={handleChange}
                className={fieldErrors.firstName ? "input-error" : ""}
              />

              {renderError("firstName")}
            </div>

            <div className="form-group">
              <label htmlFor="lastName">Last Name</label>

              <input
                id="lastName"
                name="lastName"
                type="text"
                placeholder="Enter your last name"
                value={formData.lastName}
                maxLength={LIMITS.nameMax}
                autoComplete="family-name"
                onChange={handleChange}
                className={fieldErrors.lastName ? "input-error" : ""}
              />

              {renderError("lastName")}
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="email">Email Address</label>

            <input
              id="email"
              name="email"
              type="email"
              placeholder="Enter your email"
              value={formData.email}
              maxLength={LIMITS.emailMax}
              autoComplete="email"
              onChange={handleChange}
              className={fieldErrors.email ? "input-error" : ""}
            />

            {renderError("email")}
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>

            <PasswordInput
              id="password"
              name="password"
              placeholder="E.g. Lunch2026!"
              value={formData.password}
              maxLength={LIMITS.passwordMax}
              onChange={handleChange}
            />

            {renderError("password")}
            {/* NEW: live checklist of the password rules */}
            <PasswordRules password={formData.password} />
          </div>

          <div className="form-group">
            <label htmlFor="confirmPassword">Confirm Password</label>

            <PasswordInput
              id="confirmPassword"
              name="confirmPassword"
              placeholder="Confirm your password"
              value={formData.confirmPassword}
              maxLength={LIMITS.passwordMax}
              onChange={handleChange}
            />

            {/* NEW: live "Passwords match" hint while typing (the error wins if there is one) */}
            {fieldErrors.confirmPassword ? (
              renderError("confirmPassword")
            ) : (
              <PasswordMatchHint
                password={formData.password}
                confirm={formData.confirmPassword}
              />
            )}
          </div>

          <button
            type="submit"
            className="auth-button"
            disabled={isSubmitting}
          >
            {isSubmitting ? "Creating Account..." : "Create Account"}
          </button>
        </form>

        <p className="auth-switch">
          Already have an account?{" "}
          <a
            href="#"
            onClick={(event) => {
              event.preventDefault();

              if (onGoToLogin) {
                onGoToLogin();
              }
            }}
          >
            Sign in
          </a>
        </p>
      </div>
    </div>
  );
}

export default Register;
