// src/components/AppHeader.jsx
// Shared top bar for every logged-in page (Menu, Cart, My Orders, My Profile).
// Laptop: buttons in the bar. Phone: ☰ button that opens a navy side menu.
// Help is also handled here, so every page gets the same Help popup.

import { useEffect, useState } from "react";
import "./AppHeader.css";
import HelpModal from "./HelpModal";

// The pages a student can move between
const LINKS = [
  { page: "menu", label: "Menu", icon: "🍽️" },
  { page: "cart", label: "Cart", icon: "🛒" },
  { page: "orders", label: "My Orders", icon: "🧾" },
  { page: "profile", label: "My Profile", shortLabel: "Profile", icon: "👤" },
];

// cartAlert: show the bubble on ☰ only when something new was added
// since the student last opened the cart (it clears when the cart is opened)
// onCartSeen: called when the ☰ menu is opened (you can see the cart count there)
// newOrderCount: orders placed since the student last opened My Orders
// (green "new" badge; it clears when My Orders is opened)
function AppHeader({
  user,
  current,
  cartCount = 0,
  cartAlert = false,
  onCartSeen,
  newOrderCount = 0,
  onNavigate,
  onLogout,
}) {
  const [menuOpen, setMenuOpen] = useState(false);
  const [showHelp, setShowHelp] = useState(false);

  // Close the ☰ menu with the Escape key, and stop the page scrolling behind it
  useEffect(() => {
    if (!menuOpen) return undefined;

    const onKey = (event) => {
      if (event.key === "Escape") setMenuOpen(false);
    };

    window.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";

    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [menuOpen]);

  const go = (page) => {
    setMenuOpen(false);
    if (onNavigate) onNavigate(page);
  };

  const openHelp = () => {
    setMenuOpen(false);
    setShowHelp(true);
  };

  const logout = () => {
    setMenuOpen(false);
    if (onLogout) onLogout();
  };

  return (
    <>
      <header className="app-header">
        {/* Click the logo/name to go back to the menu */}
        <button
          type="button"
          className="app-brand"
          onClick={() => go("menu")}
          aria-label="EduEats home"
        >
          <span className="app-brand-logo" aria-hidden="true">E</span>
          <span className="app-brand-text">
            <strong>EduEats</strong>
            <small>Campus Cafeteria Ordering</small>
          </span>
        </button>

        {/* Laptop: normal buttons */}
        <nav className="app-header-actions" aria-label="Main">
          {LINKS.map((link) => (
            <button
              key={link.page}
              type="button"
              className={`app-header-button ${current === link.page ? "active" : ""}`}
              aria-current={current === link.page ? "page" : undefined}
              onClick={() => go(link.page)}
            >
              {link.shortLabel || link.label}
              {link.page === "cart" && (
                <span key={cartCount} className="app-badge">
                  {cartCount}
                </span>
              )}
              {link.page === "orders" && newOrderCount > 0 && (
                <span key={newOrderCount} className="app-badge app-badge-new">
                  {newOrderCount} new
                </span>
              )}
            </button>
          ))}

          <button type="button" className="app-header-button" onClick={openHelp}>
            Help
          </button>

          <button type="button" className="app-header-button" onClick={logout}>
            Logout
          </button>
        </nav>

        {/* Phone: ☰ button (bubble = new items in the cart you haven't looked at yet) */}
        <button
          type="button"
          className="app-hamburger"
          onClick={() => {
            setMenuOpen(true);
            if (onCartSeen) onCartSeen(); // the side menu shows the cart count
          }}
          aria-label={`Open menu${cartAlert ? `, ${cartCount} in cart` : ""}${
            newOrderCount > 0 ? `, ${newOrderCount} new order${newOrderCount > 1 ? "s" : ""}` : ""
          }`}
          aria-expanded={menuOpen}
        >
          <span />
          <span />
          <span />
          {cartAlert && cartCount > 0 ? (
            <em key={cartCount} className="app-hamburger-badge">
              {cartCount}
            </em>
          ) : (
            newOrderCount > 0 && (
              // Green dot: there's a new order to look at in My Orders
              <em key={`o${newOrderCount}`} className="app-hamburger-badge new-order" />
            )
          )}
        </button>
      </header>

      {/* Phone side menu, same navy colours as the header */}
      {menuOpen && (
        <div className="app-drawer-overlay" onClick={() => setMenuOpen(false)}>
          <aside
            className="app-drawer"
            onClick={(event) => event.stopPropagation()}
            aria-label="Menu"
          >
            <div className="app-drawer-header">
              <div>
                <strong>{user?.firstName ? `Hi, ${user.firstName}` : "Menu"}</strong>
                {user?.email && <small>{user.email}</small>}
              </div>

              <button
                type="button"
                className="app-drawer-close"
                onClick={() => setMenuOpen(false)}
                aria-label="Close menu"
              >
                ×
              </button>
            </div>

            {LINKS.map((link) => (
              <button
                key={link.page}
                type="button"
                className={`app-drawer-link ${current === link.page ? "active" : ""}`}
                aria-current={current === link.page ? "page" : undefined}
                onClick={() => go(link.page)}
              >
                <span aria-hidden="true">{link.icon}</span>
                {link.label}
                {link.page === "cart" && cartCount > 0 && (
                  <span className="app-drawer-badge">{cartCount}</span>
                )}
                {link.page === "orders" && newOrderCount > 0 && (
                  <span className="app-drawer-badge new-order">{newOrderCount} new</span>
                )}
              </button>
            ))}

            <button type="button" className="app-drawer-link" onClick={openHelp}>
              <span aria-hidden="true">❓</span>
              Help
            </button>

            <button type="button" className="app-drawer-link logout" onClick={logout}>
              <span aria-hidden="true">↩</span>
              Logout
            </button>
          </aside>
        </div>
      )}

      {showHelp && <HelpModal onClose={() => setShowHelp(false)} />}
    </>
  );
}

export default AppHeader;
