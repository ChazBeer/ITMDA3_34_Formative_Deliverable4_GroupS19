import { useState } from "react";
import "./UsersPage.css";
import ConfirmDialog from "../components/ConfirmDialog";
import { setSuspended } from "./adminData";
import SortHeader from "./SortHeader";
import { useSort, timeOf } from "./useSort";

// CHANGED: one table for End-Users and Staff (StaffPage uses it too).
// Suspend / Reactivate now really works (PUT /api/users/{id}/suspend|unsuspend),
// and "Last Active" shows the user's last login.
// NEW: click a column heading to sort the table by it.
function UsersPage({
  users = [],
  loading = false,
  onViewUser,
  onUserChanged,
  breadcrumb = "EDUEATS / USER MANAGEMENT",
  title = "End-User Directory",
  noun = "users",
  showCustomerCode = true,
  addLabel = "",
  onAdd,
}) {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("All");
  const [currentPage, setCurrentPage] = useState(1);

  // NEW: suspend / reactivate
  const [confirming, setConfirming] = useState(null); // the user being suspended
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const usersPerPage = 8;

  // NEW: sorting (A→Z by name to start with)
  const sort = useSort("name");
  const sortBy = (column) => {
    sort.toggle(column);
    setCurrentPage(1);
  };
  const sortValue = (user, column) => {
    if (column === "joined") return timeOf(user.createdAt);
    if (column === "lastLogin") return timeOf(user.lastLogin);
    return user[column];
  };

  // Filter users based on search and status
  const term = search.trim().toLowerCase();
  const filteredUsers = users.filter((user) => {
    const matchesSearch =
      !term ||
      user.name.toLowerCase().includes(term) ||
      user.email.toLowerCase().includes(term) ||
      String(user.customerCode).toLowerCase().includes(term);

    const matchesStatus = statusFilter === "All" || user.status === statusFilter;

    return matchesSearch && matchesStatus;
  });
  const sortedUsers = sort.sortRows(filteredUsers, sortValue);

  const suspendedCount = users.filter((user) => user.status === "Suspended").length;

  const totalPages = Math.max(1, Math.ceil(filteredUsers.length / usersPerPage));
  const safePage = Math.min(currentPage, totalPages);
  const startIndex = (safePage - 1) * usersPerPage;
  const displayedUsers = sortedUsers.slice(startIndex, startIndex + usersPerPage);

  const changeStatus = async (user, suspend) => {
    setConfirming(null);
    setBusyId(user.id);
    setError("");
    setNotice("");

    try {
      const updated = await setSuspended(user, suspend);
      onUserChanged?.(updated);
      setNotice(
        suspend
          ? `${user.name} is suspended and can no longer log in.`
          : `${user.name} is active again and can log in.`
      );
    } catch (err) {
      setError(err.message === "Failed to fetch" ? "Cannot reach the server." : err.message);
    } finally {
      setBusyId(null);
    }
  };

  // Suspending asks first; reactivating happens straight away
  const toggleUserStatus = (user) => {
    if (user.status === "Active") setConfirming(user);
    else changeStatus(user, false);
  };

  return (
    <div className="users-page">
      {/* PAGE HEADER */}
      <div className="users-page-header">
        <div>
          <span className="users-breadcrumb">{breadcrumb}</span>
          <h2>{title}</h2>
          <p className="users-subtitle">
            {users.length} {noun}
            {suspendedCount > 0 && ` · ${suspendedCount} suspended`}
          </p>
        </div>
      </div>

      {error && (
        <div className="users-message users-message-error" role="alert">
          {error}
          <button onClick={() => setError("")}>Dismiss</button>
        </div>
      )}

      {notice && (
        <div className="users-message" role="status">
          {notice}
          <button onClick={() => setNotice("")}>Dismiss</button>
        </div>
      )}

      {/* FILTERS */}
      <section className="users-toolbar">
        <div className="users-filters">
          <label className="users-search">
            <span>⌕</span>
            <input
              type="text"
              placeholder={showCustomerCode ? "Search name, email or code..." : "Search name or email..."}
              value={search}
              maxLength={60}
              onChange={(event) => {
                setSearch(event.target.value);
                setCurrentPage(1);
              }}
            />
          </label>

          <select
            value={statusFilter}
            aria-label="Filter by status"
            onChange={(event) => {
              setStatusFilter(event.target.value);
              setCurrentPage(1);
            }}
          >
            <option value="All">Status: All</option>
            <option value="Active">Active</option>
            <option value="Suspended">Suspended</option>
          </select>
        </div>

        {onAdd && (
          <button className="users-bulk-button" onClick={onAdd}>
            + {addLabel}
          </button>
        )}
      </section>

      {/* USER TABLE */}
      <section className="users-table-container">
        <table className="users-table">
          <thead>
            <tr>
              <SortHeader label="Name" column="name" sort={sort} onSort={sortBy} />
              <SortHeader label="Email" column="email" sort={sort} onSort={sortBy} />
              <SortHeader label="Status" column="status" sort={sort} onSort={sortBy} />
              {showCustomerCode ? (
                <SortHeader label="Customer Code" column="customerCode" sort={sort} onSort={sortBy} />
              ) : (
                <SortHeader label="Date Joined" column="joined" sort={sort} onSort={sortBy} />
              )}
              <SortHeader label="Last Login" column="lastLogin" sort={sort} onSort={sortBy} />
              <th>Actions</th>
            </tr>
          </thead>

          <tbody>
            {displayedUsers.map((user) => (
              <tr key={user.id}>
                <td>
                  <button className="user-name-button" onClick={() => onViewUser(user)}>
                    <span className="user-avatar">{user.initials}</span>
                    <strong>{user.name}</strong>
                  </button>
                </td>

                <td>{user.email}</td>

                <td>
                  <span
                    className={`user-status ${
                      user.status === "Active" ? "status-active" : "status-suspended"
                    }`}
                  >
                    {user.status}
                  </span>
                </td>

                <td>{showCustomerCode ? user.customerCode : user.joined}</td>

                <td title={user.lastLogin ? new Date(user.lastLogin).toLocaleString("en-ZA") : ""}>
                  {user.lastActive}
                </td>

                <td>
                  <button className="user-action-button" onClick={() => onViewUser(user)}>
                    View
                  </button>

                  <button
                    className={`user-action-button ${
                      user.status === "Active" ? "user-action-danger" : "user-action-ok"
                    }`}
                    onClick={() => toggleUserStatus(user)}
                    disabled={busyId === user.id}
                  >
                    {busyId === user.id
                      ? "Saving..."
                      : user.status === "Active"
                        ? "Suspend"
                        : "Reactivate"}
                  </button>
                </td>
              </tr>
            ))}

            {displayedUsers.length === 0 && (
              <tr>
                <td colSpan="6" className="users-empty">
                  {loading ? `Loading ${noun}...` : `No ${noun} found.`}
                </td>
              </tr>
            )}
          </tbody>
        </table>

        {/* PAGINATION */}
        <div className="users-pagination">
          <span>
            Showing {filteredUsers.length === 0 ? 0 : startIndex + 1}
            {"–"}
            {Math.min(startIndex + usersPerPage, filteredUsers.length)} of {filteredUsers.length}{" "}
            {noun}
          </span>

          <div className="pagination-buttons">
            <button disabled={safePage === 1} onClick={() => setCurrentPage(safePage - 1)}>
              Previous
            </button>
            <span>
              {safePage} / {totalPages}
            </span>
            <button
              disabled={safePage === totalPages}
              onClick={() => setCurrentPage(safePage + 1)}
            >
              Next
            </button>
          </div>
        </div>
      </section>

      {confirming && (
        <ConfirmDialog
          title={`Suspend ${confirming.name}?`}
          message="They won't be able to log in until you reactivate the account. Their orders stay in the system."
          confirmLabel="Suspend"
          cancelLabel="Cancel"
          danger
          onConfirm={() => changeStatus(confirming, true)}
          onCancel={() => setConfirming(null)}
        />
      )}
    </div>
  );
}

export default UsersPage;
