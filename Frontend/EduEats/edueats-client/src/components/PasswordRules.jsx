// src/components/PasswordRules.jsx
// NEW: live checklist under a new-password box, so students can see which
// password rules they still need (same rules as the backend).
// Shows nothing until they start typing.

import "./PasswordRules.css";
import { PASSWORD_RULES } from "../validation";

function PasswordRules({ password }) {
  if (!password) return null;

  return (
    <ul className="password-rules" aria-live="polite">
      {PASSWORD_RULES.map((rule) => {
        const ok = rule.test(password);
        return (
          <li key={rule.key} className={ok ? "ok" : ""}>
            <span aria-hidden="true">{ok ? "✓" : "○"}</span>
            {rule.label}
            <span className="visually-hidden">{ok ? " (done)" : " (still needed)"}</span>
          </li>
        );
      })}
    </ul>
  );
}

export default PasswordRules;
