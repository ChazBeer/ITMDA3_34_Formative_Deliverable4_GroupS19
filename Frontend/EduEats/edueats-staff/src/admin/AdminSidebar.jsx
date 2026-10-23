import "./AdminDashboard.css";

const menuItems = [
  { id: "dashboard", label: "Dashboard", icon: "▦" },
  { id: "users", label: "Users", icon: "♙" },
  { id: "staff", label: "Staff", icon: "♟" },
  { id: "admins", label: "Admins", icon: "▣" },
  { id: "products", label: "Products", icon: "▤" },
  { id: "categories", label: "Categories", icon: "▦" },
  { id: "customizations", label: "Customizations", icon: "☷" },
];

// CHANGED: shows the logged-in admin and a Back link.
// Log Out moved to the top-right (same place as the staff side), so the
// small icon next to the name can't log anyone out by accident.
function AdminSidebar({ activePage, onNavigate, staffUser, onBack }) {
  const name =
    `${staffUser?.firstName || ""} ${staffUser?.lastName || ""}`.trim() ||
    "Admin";

  const initials = name
    .split(/\s+/)
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();

  return (
    <aside className="admin-sidebar">

      {/* BRAND */}
      <div className="admin-brand">
        <div className="admin-brand-logo">E</div>
        <span>EduEats</span>
      </div>

      {/* NAVIGATION */}
      <nav className="admin-menu">

        {menuItems.map((item) => (
          <button
            key={item.id}
            className={`admin-menu-item ${
              activePage === item.id ? "active" : ""
            }`}
            onClick={() => onNavigate(item.id)}
          >
            <span className="admin-menu-icon">
              {item.icon}
            </span>

            <span>{item.label}</span>
          </button>
        ))}

        {/* NEW: back to the order board */}
        <button className="admin-menu-item" onClick={onBack}>
          <span className="admin-menu-icon">←</span>
          <span>Order Board</span>
        </button>

      </nav>

      {/* ADMIN PROFILE */}
      <div className="admin-profile">

        <div className="admin-avatar">
          {initials}
        </div>

        <div className="admin-profile-info">
          <strong>{name}</strong>
          <span>Admin</span>
        </div>

      </div>

    </aside>
  );
}

export default AdminSidebar;
