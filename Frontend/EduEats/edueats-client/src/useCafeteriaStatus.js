// src/useCafeteriaStatus.js
// Asks the backend whether the cafeteria is open (GET /api/cafeteria-status)
// and whether the app can reach the server. Checks again every minute.

import { useEffect, useState } from "react";
import { apiFetch } from "./api";

const CHECK_EVERY_MS = 60000;

// "17:30:00" -> "17:30"
const shortTime = (value) => (value ? String(value).slice(0, 5) : "");

export default function useCafeteriaStatus() {
  const [status, setStatus] = useState({
    loaded: false,
    connected: true,
    isOpen: null,
    openingTime: "",
    closingTime: "",
  });

  useEffect(() => {
    let cancelled = false;

    const check = async () => {
      try {
        const response = await apiFetch("/api/cafeteria-status");

        // The server answered, so we ARE connected - we just don't know
        // the opening hours (e.g. 404 if the backend hasn't been restarted)
        if (!response.ok) {
          console.warn("Cafeteria status returned", response.status);
          if (!cancelled) {
            setStatus((current) => ({
              ...current,
              loaded: true,
              connected: true,
              isOpen: null,
            }));
          }
          return;
        }

        const data = await response.json();

        if (!cancelled) {
          setStatus({
            loaded: true,
            connected: true,
            isOpen: Boolean(data.isOpen),
            openingTime: shortTime(data.openingTime),
            closingTime: shortTime(data.closingTime),
          });
        }
      } catch (error) {
        // fetch only throws when the server can't be reached at all
        console.error("Cafeteria status error:", error);

        if (!cancelled) {
          setStatus((current) => ({
            ...current,
            loaded: true,
            connected: false,
          }));
        }
      }
    };

    check();

    const timer = setInterval(() => {
      if (document.visibilityState === "visible") check();
    }, CHECK_EVERY_MS);

    window.addEventListener("online", check);
    window.addEventListener("offline", check);

    return () => {
      cancelled = true;
      clearInterval(timer);
      window.removeEventListener("online", check);
      window.removeEventListener("offline", check);
    };
  }, []);

  return status;
}