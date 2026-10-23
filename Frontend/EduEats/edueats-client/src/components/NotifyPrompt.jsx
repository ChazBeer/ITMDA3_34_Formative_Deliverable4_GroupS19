// src/components/NotifyPrompt.jsx
// "🔔 Notify me when it's ready" button. Only shows when the browser
// supports notifications and the student hasn't answered the question yet.

import { useState } from "react";
import "./NotifyPrompt.css";
import { askNotificationPermission, notificationPermission } from "../readyNotifications";

function NotifyPrompt() {
  const [permission, setPermission] = useState(notificationPermission);

  if (permission === "granted") {
    return <p className="notify-prompt-done">🔔 We'll notify you when your order is ready.</p>;
  }

  // Not supported here, or the student said no: don't nag
  if (permission !== "default") return null;

  return (
    <button
      type="button"
      className="notify-prompt"
      onClick={async () => setPermission(await askNotificationPermission())}
    >
      🔔 Notify me when it's ready
    </button>
  );
}

export default NotifyPrompt;
