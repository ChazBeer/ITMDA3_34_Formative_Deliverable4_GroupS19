// src/components/PasswordInput.jsx
// Password box with our own Show / Hide button.
// (Edge's built-in eye icon disappears when you click away,
// so it is hidden in CSS and replaced with this one.)

import { useState } from "react";
import "./PasswordInput.css";

function PasswordInput({ id, name, value, onChange, placeholder, maxLength }) {
  const [visible, setVisible] = useState(false);

  return (
    <div className="password-field">
      <input
        id={id}
        name={name}
        type={visible ? "text" : "password"}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        maxLength={maxLength}
        autoComplete="current-password"
        required
      />

      <button
        type="button"
        className="password-toggle"
        onClick={() => setVisible((current) => !current)}
        aria-label={visible ? "Hide password" : "Show password"}
      >
        {visible ? "Hide" : "Show"}
      </button>
    </div>
  );
}

export default PasswordInput;
