// src/components/IdleWarning.jsx
// "Still there?" pop-up shown 1 minute before the idle logout.
// Tapping anywhere (or "Stay logged in") counts as activity and closes it.

import "./ConfirmDialog.css";

function IdleWarning({ secondsLeft, onLogout }) {
  return (
    <div className="confirm-overlay">
      <div
        className="confirm-box"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="idle-title"
        aria-describedby="idle-message"
      >
        <div className="confirm-icon" aria-hidden="true">⏱</div>

        <h2 id="idle-title">Still there?</h2>
        <p id="idle-message">
          For your safety you'll be logged out in <strong>{secondsLeft}s</strong>{" "}
          because there was no activity for a while.
        </p>

        <div className="confirm-actions">
          <button type="button" className="confirm-cancel" onClick={onLogout}>
            Log out
          </button>
          {/* Any tap already resets the timer, so this just needs to be tapped */}
          <button type="button" className="confirm-ok" autoFocus>
            Stay logged in
          </button>
        </div>
      </div>
    </div>
  );
}

export default IdleWarning;
