// src/components/PasswordMatchHint.jsx
// Live hint under a "Confirm password" box: tells the student straight away
// whether the two passwords match, while they type.
// Shows nothing until they start typing the confirmation.

import "./PasswordMatchHint.css";

function PasswordMatchHint({ password, confirm }) {
  if (!confirm) return null;

  const matches = password === confirm;

  return (
    <p className={`password-match ${matches ? "ok" : "no"}`} aria-live="polite">
      <span aria-hidden="true">{matches ? "✓" : "✗"}</span>
      {matches ? "Passwords match" : "Passwords don't match yet"}
    </p>
  );
}

export default PasswordMatchHint;
