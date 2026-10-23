import { useState } from "react";
import "./AdminsPage.css";
import SortHeader from "./SortHeader";
import { useSort, timeOf } from "./useSort";

// CHANGED: real ADMIN accounts come from AdminDashboard.
// "New Admin" creates a real account; admins can't be suspended (the
// backend protects them), so the row shows "Protected" instead.
// NEW: click a column heading to sort the table by it.
function AdminsPage({ admins = [], loading = false, currentUserId, onViewUser, onAdd }) {
  const [search, setSearch] = useState("");
  const sort = useSort("name");

  // Filter administrators
  const filteredAdmins = admins.filter((admin) => {
    const searchTerm = search.trim().toLowerCase();

    return (
      admin.name.toLowerCase().includes(searchTerm) ||
      admin.email.toLowerCase().includes(searchTerm)
    );
  });
  const sortedAdmins = sort.sortRows(filteredAdmins, (admin, column) =>
    column === "lastLogin" ? timeOf(admin.lastLogin) : admin[column]
  );

  return (
    <div className="admins-page">
 
      {/* PAGE HEADER */}
      <div className="admins-page-header">
        <div>
          <span className="admins-breadcrumb">
            EDUEATS / PRIVILEGES
          </span>
 
          <h2>Admin Accounts</h2>
        </div>
      </div>
 
      {/* TOOLBAR */}
      <section className="admins-toolbar">
 
        <label className="admins-search">
          <span>⌕</span>
 
          <input
            type="text"
            placeholder="Search name or email..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </label>
 
        <button
          className="admins-add-button"
          onClick={onAdd}
        >
          + New Admin
        </button>
 
      </section>
 
      {/* ADMIN TABLE */}
      <section className="admins-table-container">
 
        <table className="admins-table">
 
          <thead>
            <tr>
              <SortHeader label="Name" column="name" sort={sort} onSort={sort.toggle} />
              <SortHeader label="Email" column="email" sort={sort} onSort={sort.toggle} />
              <SortHeader label="Permission Level" column="role" sort={sort} onSort={sort.toggle} />
              <SortHeader label="Status" column="status" sort={sort} onSort={sort.toggle} />
              <SortHeader label="Last Login" column="lastLogin" sort={sort} onSort={sort.toggle} />
              <th>Actions</th>
            </tr>
          </thead>
 
          <tbody>
 
            {sortedAdmins.map((admin) => (
              <tr key={admin.id}>
 
                <td>
                  <div className="admins-name-cell">
                    <span className="admins-avatar">
                      {admin.initials}
                    </span>
 
                    <strong>{admin.name}</strong>
                  </div>
                </td>
 
                <td>{admin.email}</td>
 
                <td>
                  <span className="admins-role-badge">
                    {admin.role}
                  </span>
                </td>
 
                <td>
                  <span
                    className={`admins-status ${
                      admin.status === "Active"
                        ? "admins-status-active"
                        : admin.status === "Pending"
                        ? "admins-status-pending"
                        : "admins-status-suspended"
                    }`}
                  >
                    {admin.status}
                  </span>
                </td>
 
                <td title={admin.lastLogin ? new Date(admin.lastLogin).toLocaleString("en-ZA") : ""}>
                  {admin.lastActive}
                </td>

                <td>
                  <button className="admins-action-button" onClick={() => onViewUser(admin)}>
                    View
                  </button>
                  <span className="admins-protected" title="Admin accounts can't be suspended">
                    {admin.id === currentUserId ? "You" : "Protected"}
                  </span>
                </td>

              </tr>
            ))}
 
            {filteredAdmins.length === 0 && (
              <tr>
                <td colSpan="6" className="admins-empty">
                  {loading ? "Loading admins..." : "No administrator accounts found."}
                </td>
              </tr>
            )}
 
          </tbody>
 
        </table>
 
        <div className="admins-table-footer">
          Showing {filteredAdmins.length} of {admins.length} administrators
        </div>
 
      </section>
 
    </div>
  );
}
 
export default AdminsPage;