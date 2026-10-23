// src/passwordRules.js
// NEW: the backend's password rules, checked before sending a new password:
// 8+ characters, a capital letter, a number and a symbol.
// (Only for NEW passwords – older passwords still work for logging in.)

export const PASSWORD_HINT = "8+ characters, with a capital letter, a number and a symbol";

export function checkNewPassword(value) {
  if (value.length < 8) return "The new password needs at least 8 characters.";
  if (value.length > 64) return "The new password can be at most 64 characters.";
  if (!/[A-Z]/.test(value) || !/[0-9]/.test(value) || !/[^A-Za-z0-9\s]/.test(value)) {
    return "The password must contain at least 1 capital letter, 1 number and 1 symbol (e.g. Lunch2026!).";
  }
  return "";
}
