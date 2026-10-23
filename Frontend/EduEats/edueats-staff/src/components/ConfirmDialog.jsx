// src/components/ConfirmDialog.jsx
// App-styled "Are you sure?" pop-up (replaces the browser's window.confirm box).
// Phone: slides up from the bottom. Laptop: centred box.

import { useEffect, useRef } from "react";
import "./ConfirmDialog.css";

function ConfirmDialog({
  title,
  message,
  confirmLabel = "Yes",
  cancelLabel = "No",
  danger = false,
  onConfirm,
  onCancel,
}) {
  const cancelRef = useRef(null);
  const onCancelRef = useRef(onCancel);

  // Keep the latest onCancel for the Escape key
  useEffect(() => {
    onCancelRef.current = onCancel;
  }, [onCancel]);

  // Escape closes it, the safe button gets focus, and the page behind can't scroll.
  // Runs once when it opens (so a background refresh doesn't move the focus).
  useEffect(() => {
    const onKey = (event) => {
      if (event.key === "Escape") onCancelRef.current();
    };
    window.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    cancelRef.current?.focus();

    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, []);

  return (
    <div className="confirm-overlay" onClick={onCancel}>
      <div
        className="confirm-box"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-title"
        aria-describedby="confirm-message"
        onClick={(event) => event.stopPropagation()}
      >
        <div className={`confirm-icon ${danger ? "danger" : ""}`} aria-hidden="true">
          {danger ? "!" : "?"}
        </div>

        <h2 id="confirm-title">{title}</h2>
        <p id="confirm-message">{message}</p>

        <div className="confirm-actions">
          <button type="button" className="confirm-cancel" ref={cancelRef} onClick={onCancel}>
            {cancelLabel}
          </button>
          <button
            type="button"
            className={`confirm-ok ${danger ? "danger" : ""}`}
            onClick={onConfirm}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}

export default ConfirmDialog;
