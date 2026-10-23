// src/apiError.js
// NEW: turns the backend's error reply into one clear sentence for the user.
// The backend's error shape (GlobalExceptionHandler):
//   { timestamp, status, error, message, path, validationErrors: { field: "problem", ... } }
// When there are validationErrors we show those (they say exactly what to fix),
// otherwise the message, otherwise the short error name.

export function errorFromBody(data) {
  if (!data) return "";
  if (typeof data === "string") return data.length < 200 ? data : "";
  if (typeof data !== "object") return "";

  const details = data.validationErrors || data.errors;
  if (details && typeof details === "object") {
    const list = (Array.isArray(details) ? details : Object.values(details))
      .flat()
      .map((item) => (typeof item === "string" ? item : item?.message || item?.defaultMessage || ""))
      .filter(Boolean);
    if (list.length) {
      return list.map((text) => (/[.!?]$/.test(text) ? text : `${text}.`)).join(" ");
    }
  }

  const message = data.message;
  if (message && !/^no message available$|unexpected error/i.test(message)) return message;
  if (typeof data.error === "string" && data.error.length < 120) return data.error;
  return "";
}

// Reads the reply body (safely) and returns the message, or the fallback
export async function readApiError(response, fallback = "") {
  try {
    const text = await response.clone().text();
    if (!text) return fallback;
    try {
      return errorFromBody(JSON.parse(text)) || fallback;
    } catch {
      return text.length < 160 ? text : fallback;
    }
  } catch {
    return fallback;
  }
}
