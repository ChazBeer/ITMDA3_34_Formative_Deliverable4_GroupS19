import { useEffect, useState } from "react";
import AdminSidebar from "./AdminSidebar";
import "./AdminDashboard.css";
import UsersPage from "./UsersPage";
import UserProfile from "./UserProfile";
import StaffPage from "./StaffPage";
import AdminsPage from "./AdminsPage";
import ProductsPage from "./ProductsPage";
import CategoriesPage from "./CategoriesPage";
import CustomizationsPage from "./CustomizationsPage";
import NewAccountForm from "./NewAccountForm";
import { apiFetch } from "../api";
import {
  loadUsers,
  loadOrders,
  isEndUser,
  formatRand,
  timeAgo,
} from "./adminData";

// NEW: heading for each page
const PAGE_TITLES = {
  dashboard: ["EDUEATS / ANALYTICS", "Dashboard Overview"],
  users: ["EDUEATS / USERS", "End-Users"],
  staff: ["EDUEATS / USERS", "Staff"],
  admins: ["EDUEATS / USERS", "Admins"],
  products: ["EDUEATS / MENU", "Products & Stock"],
  categories: ["EDUEATS / MENU", "Categories"],
  customizations: ["EDUEATS / MENU", "Customizations"],
};

// NEW: items running low (stock 5 or less) for the dashboard
const LOW_STOCK = 5;
function lowStockItems(menuItems) {
  return menuItems
    .filter(
      (item) =>
        item.available !== false &&
        item.stockQuantity !== undefined &&
        item.stockQuantity !== null &&
        Number(item.stockQuantity) <= LOW_STOCK
    )
    .sort((a, b) => Number(a.stockQuantity) - Number(b.stockQuantity));
}

// NEW: statuses that count as "active" (not finished)
const FINISHED = ["collected", "cancelled"];

// NEW: orders per day for the last 7 days, for the chart
function ordersPerDay(orders) {
  const days = [];

  for (let i = 6; i >= 0; i--) {
    const day = new Date();
    day.setHours(0, 0, 0, 0);
    day.setDate(day.getDate() - i);
    days.push({
      label: day.toLocaleDateString("en-ZA", { weekday: "short" }),
      start: day.getTime(),
      count: 0,
    });
  }

  orders.forEach((order) => {
    const time = new Date(order.createdAt).getTime();
    const match = days.find(
      (day) => time >= day.start && time < day.start + 86400000
    );
    if (match) match.count += 1;
  });

  return days;
}

// CHANGED: receives the logged-in admin, plus Back and Log Out
function AdminDashboard({ staffUser, onBack, onLogout }) {
  const [activePage, setActivePage] = useState("dashboard");
  const [selectedUser, setSelectedUser] = useState(null);

  // NEW: "New Staff / Admin account" pop-up, and Categories -> Products filter
  const [newAccount, setNewAccount] = useState(null); // { roleName, roleLabel }
  const [productSearch, setProductSearch] = useState("");
  const [notice, setNotice] = useState("");
  const [menuItems, setMenuItems] = useState([]);

  // NEW: real data from the backend
  const [users, setUsers] = useState([]);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const refresh = async () => {
    setLoading(true);
    try {
      const [userList, orderList, menuResponse] = await Promise.all([
        loadUsers(),
        loadOrders(),
        apiFetch("/api/menu-items/admin/all").catch(() => null),
      ]);
      setUsers(userList);
      setOrders(orderList);
      // Stock alerts are extra: the dashboard still works without them
      if (menuResponse?.ok) setMenuItems(await menuResponse.json());
      setError("");
    } catch (err) {
      console.error("Admin data error:", err);
      setError(
        err.message === "Failed to fetch"
          ? "Cannot reach the server. Is the backend running?"
          : err.message
      );
    } finally {
      setLoading(false);
    }
  };

  // First load straight after the page appears
  useEffect(() => {
    const first = setTimeout(refresh, 0);
    return () => clearTimeout(first);
  }, []);

  // NEW: a user was suspended / reactivated: update the lists and the profile
  const handleUserChanged = (updated) => {
    setUsers((list) =>
      list.map((user) => (user.id === updated.id ? { ...user, ...updated } : user))
    );
    setSelectedUser((current) =>
      current && current.id === updated.id ? { ...current, ...updated } : current
    );
  };

  const handleAccountCreated = (account) => {
    setUsers((list) => [...list, account]);
    setNotice(
      `${account.name} can now log in on the staff app with ${account.email} and the temporary password.`
    );
    setNewAccount(null);
  };

  const goTo = (page) => {
    setActivePage(page);
    setSelectedUser(null);
    setNotice("");
    if (page !== "products") setProductSearch("");
  };

  const [breadcrumb, pageTitle] = PAGE_TITLES[activePage] || PAGE_TITLES.dashboard;
  const lowStock = lowStockItems(menuItems);

  const endUsers = users.filter(isEndUser);
  const staffMembers = users.filter((user) => user.role === "STAFF");
  const adminAccounts = users.filter((user) => user.role === "ADMIN");
  const activeOrders = orders.filter(
    (order) => !FINISHED.includes((order.status || "").toLowerCase())
  );

  // CHANGED: real counts (was fixed sample numbers)
  const statistics = [
    {
      title: "Total End-Users",
      value: endUsers.length,
      description: "students & customers",
      icon: "♙",
    },
    {
      title: "Operational Staff",
      value: staffMembers.length,
      description: "staff accounts",
      icon: "♟",
    },
    {
      title: "Admin Accounts",
      value: adminAccounts.length,
      description: "admin accounts",
      icon: "▣",
    },
    {
      title: "Active Orders",
      value: activeOrders.length,
      description: `of ${orders.length} orders in total`,
      icon: "▤",
    },
  ];

  // CHANGED: latest orders (was a fixed sample log)
  const activities = [...orders]
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .slice(0, 5)
    .map((order) => ({
      icon: "▤",
      title: `${order.customerOrderNumber || `Order #${order.orderId}`} · ${
        order.status
      } · ${formatRand(order.totalPrice)}`,
      time: timeAgo(order.createdAt),
    }));

  // CHANGED: chart drawn from real orders of the last 7 days
  const days = ordersPerDay(orders);
  const chartMax = Math.max(4, ...days.map((day) => day.count));
  const points = days.map((day, index) => {
    const x = Math.round((index / 6) * 600);
    const y = Math.round(210 - (day.count / chartMax) * 190);
    return `${x},${y}`;
  });
  const todayCount = days[days.length - 1].count;

  return (
    <div className="admin-layout">

      {/* SIDEBAR */}
      <AdminSidebar
        activePage={activePage}
        onNavigate={goTo}
        staffUser={staffUser}
        onBack={onBack}
      />

      {/* MAIN AREA */}
      <div className="admin-main">

        {/* TOPBAR */}
        <header className="admin-topbar">

          <div className="admin-page-heading">
            <span>{breadcrumb}</span>
            <h1>{selectedUser ? "User Profile" : pageTitle}</h1>
          </div>

          <div className="admin-topbar-actions">

            {/* NEW: back to the order board */}
            <button
              className="admin-notification"
              style={{ width: "auto", padding: "0 14px" }}
              onClick={onBack}
            >
              ← Orders
            </button>

            <button
              className="admin-notification"
              style={{ width: "auto", padding: "0 14px" }}
              onClick={refresh}
              disabled={loading}
            >
              {loading ? "Loading..." : "Refresh"}
            </button>

            {/* CHANGED: Log Out top-right, same place as on the staff side */}
            <button type="button" className="admin-logout-button" onClick={onLogout}>
              Log Out
            </button>

          </div>

        </header>

        {/* CONTENT */}
        <main className="admin-content">

          {notice && (
            <div className="admin-notice" role="status">
              {notice}
              <button onClick={() => setNotice("")}>Dismiss</button>
            </div>
          )}

          {/* NEW: error message */}
          {error && (
            <section
              className="admin-placeholder"
              style={{ color: "#b91c1c", marginBottom: "20px" }}
            >
              <p>{error}</p>
            </section>
          )}

          {activePage === "dashboard" && (
            <>

              {/* STATISTICS */}
              <section className="stats-grid">

                {statistics.map((stat) => (
                  <article
                    className="stat-card"
                    key={stat.title}
                  >

                    <div className="stat-card-top">
                      <span>{stat.title}</span>

                      <div className="stat-icon">
                        {stat.icon}
                      </div>
                    </div>

                    <h2>{loading ? "…" : stat.value}</h2>

                    <div className="stat-change">
                      <span>{stat.description}</span>
                    </div>

                  </article>
                ))}

              </section>

              {/* LOWER DASHBOARD */}
              <section className="dashboard-widgets">

                {/* ORDERS CHART */}
                <article className="admin-widget registration-widget">

                  <div className="widget-heading">
                    <div>
                      <h2>Orders Trend</h2>
                      <p>
                        Orders placed in the last 7 days
                      </p>
                    </div>

                    <span className="widget-tag">
                      +{todayCount} today
                    </span>
                  </div>

                  <div className="chart-container">

                    <div className="chart-y-axis">
                      <span>{chartMax}</span>
                      <span>{Math.round(chartMax * 0.75)}</span>
                      <span>{Math.round(chartMax * 0.5)}</span>
                      <span>{Math.round(chartMax * 0.25)}</span>
                      <span>0</span>
                    </div>

                    <div className="chart-area">

                      <div className="chart-grid-lines">
                        <span />
                        <span />
                        <span />
                        <span />
                        <span />
                      </div>

                      <svg
                        className="registration-chart"
                        viewBox="0 0 600 220"
                        preserveAspectRatio="none"
                      >
                        <defs>
                          <linearGradient
                            id="chartGradient"
                            x1="0"
                            y1="0"
                            x2="0"
                            y2="1"
                          >
                            <stop
                              offset="0%"
                              stopColor="#6366f1"
                              stopOpacity="0.15"
                            />

                            <stop
                              offset="100%"
                              stopColor="#6366f1"
                              stopOpacity="0"
                            />
                          </linearGradient>
                        </defs>

                        <path
                          d={`M ${points.join(" L ")} L 600 220 L 0 220 Z`}
                          fill="url(#chartGradient)"
                        />

                        <polyline
                          points={points.join(" ")}
                          fill="none"
                          stroke="#6366f1"
                          strokeWidth="3"
                          strokeLinejoin="round"
                          strokeLinecap="round"
                        />
                      </svg>

                    </div>

                  </div>

                  <div className="chart-labels">
                    {days.map((day) => (
                      <span key={day.start}>{day.label}</span>
                    ))}
                  </div>

                </article>

                {/* ACTIVITY LOG */}
                <article className="admin-widget activity-widget">

                  <div className="widget-heading">
                    <div>
                      <h2>Recent Orders</h2>
                      <p>Latest orders placed</p>
                    </div>
                  </div>

                  <div className="activity-list">

                    {activities.map((activity, index) => (
                      <div
                        className="activity-item"
                        key={index}
                      >

                        <div className="activity-icon">
                          {activity.icon}
                        </div>

                        <div className="activity-details">
                          <p>{activity.title}</p>
                          <span>{activity.time}</span>
                        </div>

                      </div>
                    ))}

                    {!loading && activities.length === 0 && (
                      <p>No orders yet.</p>
                    )}

                  </div>

                </article>

              </section>

              {/* NEW: stock alerts */}
              <section className="admin-widget stock-widget">
                <div className="widget-heading">
                  <div>
                    <h2>Stock Alerts</h2>
                    <p>Available items with {LOW_STOCK} or fewer left</p>
                  </div>
                  <button className="stock-widget-link" onClick={() => goTo("products")}>
                    Manage stock →
                  </button>
                </div>

                {lowStock.length === 0 ? (
                  <p className="stock-widget-empty">
                    {loading ? "Loading..." : "✓ Nothing is running low."}
                  </p>
                ) : (
                  <ul className="stock-widget-list">
                    {lowStock.slice(0, 8).map((item) => (
                      <li key={item.menuItemId}>
                        <span>{item.name}</span>
                        <strong className={Number(item.stockQuantity) === 0 ? "out" : "low"}>
                          {Number(item.stockQuantity) === 0
                            ? "Sold out"
                            : `${item.stockQuantity} left`}
                        </strong>
                      </li>
                    ))}
                  </ul>
                )}
                {lowStock.length > 8 && (
                  <p className="stock-widget-more">and {lowStock.length - 8} more</p>
                )}
              </section>

            </>
          )}

          {/* User profile (from Users, Staff or Admins) */}
          {selectedUser && (
            <UserProfile
              user={selectedUser}
              currentUserId={staffUser?.userId}
              onBack={() => setSelectedUser(null)}
              onUserChanged={handleUserChanged}
            />
          )}

          {!selectedUser && activePage === "users" && (
            <UsersPage
              users={endUsers}
              loading={loading}
              onViewUser={setSelectedUser}
              onUserChanged={handleUserChanged}
            />
          )}

          {!selectedUser && activePage === "staff" && (
            <StaffPage
              staff={staffMembers}
              loading={loading}
              onViewUser={setSelectedUser}
              onUserChanged={handleUserChanged}
              onAdd={() => setNewAccount({ roleName: "STAFF", roleLabel: "Staff" })}
            />
          )}

          {!selectedUser && activePage === "admins" && (
            <AdminsPage
              admins={adminAccounts}
              loading={loading}
              currentUserId={staffUser?.userId}
              onViewUser={setSelectedUser}
              onAdd={() => setNewAccount({ roleName: "ADMIN", roleLabel: "Admin" })}
            />
          )}

          {activePage === "products" && (
            <ProductsPage key={productSearch} initialSearch={productSearch} />
          )}

          {activePage === "categories" && (
            <CategoriesPage
              onViewItems={(categoryName) => {
                setProductSearch(categoryName);
                setActivePage("products");
              }}
            />
          )}

          {activePage === "customizations" && <CustomizationsPage />}

        </main>

      </div>

      {newAccount && (
        <NewAccountForm
          roleName={newAccount.roleName}
          roleLabel={newAccount.roleLabel}
          onCreated={handleAccountCreated}
          onClose={() => setNewAccount(null)}
        />
      )}

    </div>
  );
}

export default AdminDashboard;
