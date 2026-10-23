// src/api.js
// One place for talking to the backend.
//
// - Adds the saved access token to every request
// - Sends the refresh-token cookie (credentials: "include")
// - If a request gets 401 (access token expired), it asks the backend
//   for a new access token ONCE, then repeats the request
// - If refreshing fails, the 401 is passed back so the page can log out

// Where the backend lives.
// - Running locally (npm run dev): leave VITE_API_URL unset. The app then uses
//   its own address (localhost:5173, or 192.168.x.x:5173 on a phone) and Vite
//   forwards /api to the backend on this laptop (see vite.config.js).
// - Hosted online: set VITE_API_URL to the backend's address before building,
//   e.g. VITE_API_URL=https://edueats-api.example.com  (no slash at the end).
//   See .env.example.
export const API_BASE_URL = (import.meta.env.VITE_API_URL || "").trim().replace(/\/+$/, "");

// Messages for when the backend can't be reached or breaks
export const SERVER_UNREACHABLE_MESSAGE =
  "Can't connect to the EduEats server right now. Check your internet connection and try again in a moment.";
export const SERVER_ERROR_MESSAGE =
  "Something went wrong on the server. Please try again in a moment.";

// 502 / 503 / 504 = the backend is down or restarting
// (the Vite proxy or the host answers instead of the backend)
export const isServerDownStatus = (status) =>
  status === 502 || status === 503 || status === 504;

// Sent when a refreshed login belongs to a DIFFERENT account than the one
// this app is signed in as. Happens when the student and staff apps run in
// the same browser on localhost: they share the refresh cookie, so the
// backend hands back whoever logged in last. App.jsx listens and logs out.
export const SESSION_MISMATCH_EVENT = "edueats:session-mismatch";

// The account a token belongs to ("sub" = the email address)
function tokenEmail(token) {
  try {
    const part = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return String(JSON.parse(atob(part)).sub || "").toLowerCase();
  } catch {
    return "";
  }
}

function signedInEmail() {
  try {
    return String(JSON.parse(localStorage.getItem("user"))?.email || "").toLowerCase();
  } catch {
    return "";
  }
}

// If several requests expire at the same time, they all wait
// for the same refresh instead of each starting their own.
let refreshPromise = null;

async function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
          method: "POST",
          credentials: "include",
        });

        if (!response.ok) {
          return null;
        }

        const data = await response.json();
        const newToken = data.token;

        // Never switch accounts silently (e.g. to the staff login)
        const expected = signedInEmail();
        const actual = newToken ? tokenEmail(newToken) : "";
        if (expected && actual && expected !== actual) {
          console.warn("Refreshed login belongs to another account - signing out.");
          window.dispatchEvent(new Event(SESSION_MISMATCH_EVENT));
          return null;
        }

        if (newToken) {
          localStorage.setItem("token", newToken);
        }

        return newToken || null;
      } catch (error) {
        console.error("Token refresh failed:", error);
        return null;
      } finally {
        refreshPromise = null;
      }
    })();
  }

  return refreshPromise;
}

// Seconds until a login token runs out (null if it can't be read).
// The middle part of a JWT is plain JSON with "exp" (expiry time in seconds).
function secondsLeft(token) {
  try {
    const part = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const { exp } = JSON.parse(atob(part));
    return typeof exp === "number" ? exp - Date.now() / 1000 : null;
  } catch {
    return null;
  }
}

// Gives back a token that is still valid, refreshing it first if it has
// (almost) run out. Used by the live-updates connection, which doesn't go
// through apiFetch, so it would otherwise reconnect with an old token.
export async function getFreshToken() {
  const token = localStorage.getItem("token");
  if (!token) return null;

  const left = secondsLeft(token);
  if (left !== null && left < 30) {
    return (await refreshAccessToken()) || null;
  }

  return token;
}

function buildOptions(options, token) {
  const headers = { ...(options.headers || {}) };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return {
    ...options,
    headers,
    credentials: "include",
  };
}

// Use instead of fetch() for any backend call that needs the user to be logged in.
// path example: "/api/menu-items"
export async function apiFetch(path, options = {}) {
  const url = `${API_BASE_URL}${path}`;
  const token = localStorage.getItem("token");

  let response = await fetch(url, buildOptions(options, token));

  if (response.status === 401) {
    const newToken = await refreshAccessToken();

    if (newToken) {
      // Try the same request again with the new token
      response = await fetch(url, buildOptions(options, newToken));
    }
  }

  return response;
}

// Tells the backend to cancel the access token and the refresh token.
// Never throws: logging out on the frontend must always work.
export async function logoutFromServer() {
  const token = localStorage.getItem("token");
  const url = `${API_BASE_URL}/api/auth/logout`;

  try {
    let response = await fetch(url, {
      method: "POST",
      credentials: "include",
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });

    // If the access token was expired or invalid, the backend rejects
    // the request before logging out. Try again WITHOUT the token so the
    // refresh-token cookie still gets cancelled.
    if (!response.ok && token) {
      response = await fetch(url, {
        method: "POST",
        credentials: "include",
      });
    }

    if (!response.ok) {
      console.warn("Server logout returned", response.status);
    }
  } catch (error) {
    console.error("Server logout failed:", error);
  }
}
