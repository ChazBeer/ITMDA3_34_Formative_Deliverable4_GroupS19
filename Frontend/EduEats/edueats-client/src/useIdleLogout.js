// src/useIdleLogout.js
// Logs the student out after 15 minutes without any activity
// (no taps, clicks, typing or scrolling), with a 1-minute warning first.
//
// - The last activity time is kept in localStorage, so it is shared by all
//   open tabs and still counts if the browser was closed and reopened.
// - Phones pause timers when the screen is off, so it also checks the
//   moment the student comes back to the app.
// - Waiting for food doesn't count as idle: before warning, it asks
//   keepAlive() (e.g. "does this student have an order in progress?")
//   and if so, starts the 15 minutes again.
// - This is a frontend safety net; the backend's token expiry is the real lock.

import { useEffect, useRef, useState } from "react";

export const IDLE_LIMIT_MS = 15 * 60 * 1000;
const WARNING_MS = 60 * 1000; // show "Still there?" for the last minute
const CHECK_EVERY_MS = 1000;
const SAVE_EVERY_MS = 5000; // don't write to storage on every scroll event

const LAST_ACTIVE_KEY = "edueatsLastActive";
const ACTIVITY_EVENTS = ["pointerdown", "keydown", "wheel", "touchstart", "scroll"];

function readLastActive() {
  try {
    return Number(localStorage.getItem(LAST_ACTIVE_KEY)) || 0;
  } catch {
    return 0;
  }
}

function saveLastActive(time) {
  try {
    localStorage.setItem(LAST_ACTIVE_KEY, String(time));
  } catch {
    // storage blocked - the timer then only works while this tab is open
  }
}

// Call after logging in, so an old time from a previous visit doesn't count
export function resetIdleClock() {
  saveLastActive(Date.now());
}

// Call when logging out
export function clearIdleClock() {
  try {
    localStorage.removeItem(LAST_ACTIVE_KEY);
  } catch {
    // nothing to clear
  }
}

// enabled: only runs while logged in
// onTimeout: called once when the time is up (log out there)
// keepAlive (optional): async () => true to stay logged in anyway
// Returns the seconds left while the warning should show, otherwise null
function useIdleLogout(enabled, onTimeout, keepAlive) {
  const [secondsLeft, setSecondsLeft] = useState(null);

  const onTimeoutRef = useRef(onTimeout);
  const keepAliveRef = useRef(keepAlive);
  useEffect(() => {
    onTimeoutRef.current = onTimeout;
    keepAliveRef.current = keepAlive;
  }, [onTimeout, keepAlive]);

  useEffect(() => {
    if (!enabled) return undefined;

    let lastActive = readLastActive();
    let lastSaved = lastActive;
    let timedOut = false;
    let keepAliveState = "not-asked"; // "not-asked" | "asking" | "said-no"

    // Opened again after being away too long (e.g. the tab was closed):
    // log out straight away. Otherwise start counting from now.
    const startedStale = lastActive > 0 && Date.now() - lastActive >= IDLE_LIMIT_MS;
    if (!startedStale) {
      lastActive = Date.now();
      lastSaved = lastActive;
      saveLastActive(lastActive);
    }

    const onActivity = () => {
      if (timedOut) return;
      lastActive = Date.now();
      keepAliveState = "not-asked";
      setSecondsLeft(null); // hide the warning straight away
      if (lastActive - lastSaved >= SAVE_EVERY_MS) {
        lastSaved = lastActive;
        saveLastActive(lastActive);
      }
    };

    const check = () => {
      if (timedOut) return;

      // Another tab may have been used more recently
      lastActive = Math.max(lastActive, readLastActive());
      const idleFor = Date.now() - lastActive;

      // Nearly time: first ask if there's a reason to stay (only once per idle spell)
      if (idleFor >= IDLE_LIMIT_MS - WARNING_MS && keepAliveRef.current) {
        if (keepAliveState === "asking") return; // wait for the answer
        if (keepAliveState === "not-asked") {
          keepAliveState = "asking";
          Promise.resolve()
            .then(() => keepAliveRef.current())
            .catch(() => false)
            .then((stay) => {
              if (timedOut) return;
              if (stay) {
                // e.g. food is being made: start the 15 minutes again
                lastActive = Date.now();
                lastSaved = lastActive;
                saveLastActive(lastActive);
                keepAliveState = "not-asked";
              } else {
                keepAliveState = "said-no";
              }
              check();
            });
          return;
        }
      }

      if (idleFor >= IDLE_LIMIT_MS) {
        timedOut = true;
        setSecondsLeft(null);
        onTimeoutRef.current?.();
        return;
      }

      const left = IDLE_LIMIT_MS - idleFor;
      setSecondsLeft(left <= WARNING_MS ? Math.ceil(left / 1000) : null);
    };

    const onVisible = () => {
      if (document.visibilityState === "visible") check();
    };

    ACTIVITY_EVENTS.forEach((name) =>
      window.addEventListener(name, onActivity, { passive: true, capture: true })
    );
    document.addEventListener("visibilitychange", onVisible);
    const timer = setInterval(check, CHECK_EVERY_MS);
    const first = setTimeout(check, 0);

    return () => {
      ACTIVITY_EVENTS.forEach((name) =>
        window.removeEventListener(name, onActivity, { capture: true })
      );
      document.removeEventListener("visibilitychange", onVisible);
      clearInterval(timer);
      clearTimeout(first);
    };
  }, [enabled]);

  // Not logged in: never show the warning
  return enabled ? secondsLeft : null;
}

export default useIdleLogout;
