// src/components/StaffHeader.jsx
// The navy top bar, the same on the Orders board and Settings:
// logo, page links (Orders, Settings, Admin Panel for admins),
// connection status, the signed-in name and Log Out.
// CHANGED: the name is bigger and opens Account Settings when clicked.

function StaffHeader({ page, isAdmin, connection, staffName, fullName, onNavigate, onLogout }) {
  const links = [
    { page: "board", label: "Orders", icon: "◉" },
    { page: "settings", label: "Settings", icon: "⚙" },
    ...(isAdmin ? [{ page: "admin", label: "Admin Panel", icon: "★" }] : []),
  ];

  // connection: "live" (instant updates), "online" (backup refresh only), "offline"
  const status = {
    live: { text: "● Live updates", className: "" },
    online: { text: "● Connected", className: "is-online" },
    offline: { text: "● Offline", className: "is-offline" },
  }[connection];

  return (
    <header className="topbar">
      <div className="brand">
        <div className="brand-logo">E</div>
        <span>
          EduEats <strong>staff</strong>
        </span>
      </div>

      <nav className="topbar-nav" aria-label="Main">
        {links.map((link) => (
          <button
            key={link.page}
            type="button"
            className={`nav-link ${page === link.page ? "active" : ""}`}
            aria-current={page === link.page ? "page" : undefined}
            onClick={() => page !== link.page && onNavigate(link.page)}
          >
            {link.icon} <span>{link.label}</span>
          </button>
        ))}
      </nav>

      <div className="topbar-right">
        {status && (
          <span className={`terminal-status ${status.className}`} role="status">
            {status.text}
          </span>
        )}

        <button
          type="button"
          className="user-chip"
          onClick={() => page !== "settings" && onNavigate("settings")}
          title="Account settings"
          aria-label={`${fullName || staffName}: account settings`}
        >
          <span className="user-avatar" aria-hidden="true">
            {staffName.charAt(0).toUpperCase()}
          </span>
          <span className="user-name">{fullName || staffName}</span>
        </button>

        <button type="button" className="staff-login" onClick={onLogout}>
          Log Out
        </button>
      </div>
    </header>
  );
}

export default StaffHeader;
