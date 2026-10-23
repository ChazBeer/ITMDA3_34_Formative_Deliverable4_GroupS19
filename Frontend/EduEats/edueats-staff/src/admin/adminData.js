// src/admin/adminData.js
// Shared helpers for the Admin Panel: load users and orders from the backend
// and turn them into the shapes the admin pages already use.
 
import { apiFetch } from "../api";
import { errorFromBody } from "../apiError";
 
// The role may come back as "ADMIN" or as { roleName: "ADMIN" }
export function getRoleName(user) {
  const role =
    user?.role?.roleName || user?.role?.name || user?.roleName || user?.role || "";
  return String(role).toUpperCase();
}
 
export function isEndUser(user) {
  const role = getRoleName(user);
  return role === "CUSTOMER" || role === "STUDENT";
}
 
// Turn a backend user into the shape the tables use
export function toAdminUser(user) {
  const name =
    `${user.firstName || ""} ${user.lastName || ""}`.trim() || user.email;
 
  const initials = name
    .split(/\s+/)
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
 
  return {
    id: user.userId,
    name,
    email: user.email,
    role: getRoleName(user),
    customerCode: user.customerCode || "—",
    phone: user.phoneNumber || "—",
    status: user.active === false ? "Suspended" : "Active",
    active: user.active !== false,
    createdAt: user.createdAt,
    joined: formatDate(user.createdAt),
    // NEW: last time they logged in (from /api/users/profiles)
    lastLogin: user.lastLogin || null,
    lastActive: user.lastLogin ? timeAgo(user.lastLogin) : "Never",
    initials,
  };
}

// NEW: the backend's error text, e.g. "Cannot suspend an administrative account."
export async function readError(response, fallback) {
  if (response.status === 401) return "Your session has ended. Please log in again.";
  if (response.status === 403) return "Only ADMIN accounts can do this.";
  try {
    // CHANGED: shows validationErrors (e.g. "Price must be greater than 0.") too
    const text = errorFromBody(await response.json());
    if (text) return text;
  } catch {
    // no JSON in the reply
  }
  return `${fallback} (status ${response.status}).`;
}
 
async function readJson(response, what) {
  if (response.status === 401) {
    throw new Error("Your session has ended. Please log in again.");
  }
  if (response.status === 403) {
    throw new Error(`Only ADMIN accounts can view ${what}.`);
  }
  if (!response.ok) {
    throw new Error(`Could not load ${what} (status ${response.status}).`);
  }
  return response.json();
}
 
// Users plus their last login. Last login comes from /api/users/profiles;
// if that fails the list still loads (last login shows "Never").
export async function loadUsers() {
  const [usersResponse, profilesResponse] = await Promise.all([
    apiFetch("/api/users"),
    apiFetch("/api/users/profiles").catch(() => null),
  ]);
  const data = await readJson(usersResponse, "users");

  const lastLogins = new Map();
  if (profilesResponse?.ok) {
    try {
      (await profilesResponse.json()).forEach((profile) =>
        lastLogins.set(profile.userId, profile.lastLogin)
      );
    } catch {
      // ignore - last login is extra information
    }
  }

  return data.map((user) =>
    toAdminUser({ ...user, lastLogin: lastLogins.get(user.userId) ?? user.lastLogin })
  );
}

// NEW: suspend (suspend = true) or reactivate a STUDENT/CUSTOMER/STAFF account.
// The backend doesn't allow suspending admins.
export async function setSuspended(user, suspend) {
  const response = await apiFetch(
    `/api/users/${user.id}/${suspend ? "suspend" : "unsuspend"}`,
    { method: "PUT" }
  );
  if (!response.ok) {
    throw new Error(
      await readError(response, `Could not ${suspend ? "suspend" : "reactivate"} ${user.name}`)
    );
  }
  // keep the last login we already know (the reply doesn't include it)
  return toAdminUser({ ...(await response.json()), lastLogin: user.lastLogin });
}

// NEW: looks up a role's id by its name (GET /api/roles), because the ids
// are not the same in every copy of the database (e.g. ADMIN is 1 in one
// database and 3 in another).
export async function findRoleId(roleName) {
  const response = await apiFetch("/api/roles");
  if (!response.ok) {
    throw new Error(await readError(response, "Could not load the roles"));
  }
  const roles = await response.json();
  const match = roles.find(
    (role) => String(role.roleName || role.name).toUpperCase() === roleName
  );
  if (!match) throw new Error(`The ${roleName} role was not found in the database.`);
  return match.roleId ?? match.id;
}

// NEW: create a staff or admin account (POST /api/users?roleId=...)
// roleName: "STAFF" or "ADMIN"
export async function createAccount(details, roleName) {
  const roleId = await findRoleId(roleName);
  const response = await apiFetch(`/api/users?roleId=${roleId}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      firstName: details.firstName.trim(),
      lastName: details.lastName.trim(),
      email: details.email.trim(),
      phoneNumber: details.phoneNumber.trim() || null,
      passwordHash: details.password,
    }),
  });
  if (!response.ok) {
    throw new Error(await readError(response, "Could not create the account"));
  }
  return toAdminUser(await response.json());
}

// NEW: give a user a new password (the admin's own password is checked first)
export async function resetPassword(user, adminPassword, newPassword) {
  const response = await apiFetch(`/api/users/${user.id}/reset-password`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ adminPassword, newPassword }),
  });
  if (!response.ok) {
    throw new Error(await readError(response, "Could not reset the password"));
  }
}
 
export async function loadOrders() {
  return readJson(await apiFetch("/api/orders"), "orders");
}
 
export async function loadOrdersForUser(userId) {
  return readJson(
    await apiFetch(`/api/orders/user/${userId}`),
    "this user's orders"
  );
}
 
export const formatRand = (amount) => `R${Number(amount ?? 0).toFixed(2)}`;
 
export function formatDate(value) {
  return value
    ? new Date(value).toLocaleDateString("en-ZA", {
        day: "2-digit",
        month: "short",
        year: "numeric",
      })
    : "—";
}
 
export function timeAgo(value) {
  if (!value) return "";
  const minutes = Math.max(
    0,
    Math.floor((Date.now() - new Date(value).getTime()) / 60000)
  );
  if (minutes < 1) return "just now";
  if (minutes < 60) return `${minutes} mins ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} hour${hours === 1 ? "" : "s"} ago`;
  const days = Math.floor(hours / 24);
  return `${days} day${days === 1 ? "" : "s"} ago`;
}
 