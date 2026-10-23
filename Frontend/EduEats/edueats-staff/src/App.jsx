import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import "./App.css";
// Phone & small-tablet layout (was never loaded before)
import "./mobile.css";
import Login from "./components/Login";
import StaffHeader from "./components/StaffHeader";
import ManualOrder from "./components/ManualOrder";
import ConfirmDialog from "./components/ConfirmDialog";
import StatsBar from "./components/StatsBar";
import KitchenView from "./components/KitchenView";
import SoldOutPanel from "./components/SoldOutPanel";
import ItemThumb from "./components/ItemThumb";
import useMenuItems from "./menuItems";
import AccountSettings from "./components/AccountSettings";
import AdminDashboard from "./admin/AdminDashboard";
import { readApiError } from "./apiError";
import { apiFetch, logoutFromServer } from "./api";
import useKitchenUpdates from "./useKitchenUpdates";
import { isSoundOn, playNewOrderSound, saveSoundOn } from "./newOrderSound";
import {
  AUTO_CANCEL_MINUTES,
  STATUSES,
  clockTime,
  formatPrice,
  isToday,
  itemsSummary,
  minutesSince,
  timeAgo,
  toBoardOrder,
} from "./orders";

// Backup check for new orders. Live updates normally do the work,
// so it runs slowly while they're connected and quickly if they drop.
const REFRESH_LIVE_MS = 60000;
const REFRESH_OFFLINE_MS = 15000;

// How many finished orders the Completed column shows before "Show all"
const COMPLETED_SHOWN = 10;

// How long a brand-new order card stays highlighted
const NEW_HIGHLIGHT_MS = 12000;

// New orders that haven't been accepted get a warning this many
// minutes before the backend auto-cancels them
const WARN_BEFORE_CANCEL_MINUTES = 5;

const FILTERS = ["All Orders", ...STATUSES, "Cancelled"];

// PERFORMANCE: backend status shown on the card straight away when a button is clicked
const OPTIMISTIC_STATUS = {
  "In Progress": "Preparing",
  Ready: "Ready",
  Completed: "Collected",
  New: "Accepted",
};

function getStatusSteps(currentBackendStatus, targetColumn) {
  const current = currentBackendStatus.toLowerCase();

  if (targetColumn === "In Progress") {
    return current === "submitted" ? ["Accepted", "Preparing"] : ["Preparing"];
  }

  if (targetColumn === "Ready") return ["Ready"];
  if (targetColumn === "Completed") return ["Collected"];
  if (targetColumn === "New") return ["Accepted"];

  return [];
}

function hasSavedSession() {
  const token = localStorage.getItem("token");
  const remember = localStorage.getItem("staffRemember") !== "false";
  const sameTab = sessionStorage.getItem("staffSessionActive") === "true";

  return Boolean(token) && (remember || sameTab);
}

// CHANGED: uses the shared reader (shows validationErrors too)
function readError(response, fallback) {
  return readApiError(response, fallback);
}

// The words on the "next step" button for each column
const NEXT_LABEL = {
  New: "Start Preparing →",
  "In Progress": "Mark Ready →",
  Ready: "Complete Order →",
};

function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(hasSavedSession);

  const [staffUser, setStaffUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem("staffUser"));
    } catch {
      return null;
    }
  });

  const [orders, setOrders] = useState([]);
  const [loadingOrders, setLoadingOrders] = useState(false);
  const [ordersError, setOrdersError] = useState("");
  const [connected, setConnected] = useState(true);
  const [updatingId, setUpdatingId] = useState(null);

  const [search, setSearch] = useState("");
  const [activeFilter, setActiveFilter] = useState("All Orders");
  const [view, setView] = useState("board");
  const [showAdmin, setShowAdmin] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const pageRef = useRef("board");
  const handlingPopStateRef = useRef(false);

  // NEW: manual orders, cancelling, messages, new-order alerts
  const [showManualOrder, setShowManualOrder] = useState(false);
  const [cancelTarget, setCancelTarget] = useState(null);
  const [notice, setNotice] = useState(null); // { type: "success" | "error", text, id }
  const [freshIds, setFreshIds] = useState([]); // orders that just arrived
  const [unseenCount, setUnseenCount] = useState(0); // shown in the tab title
  const [soundOn, setSoundOn] = useState(isSoundOn);

  // NEW: kitchen display, sold-out panel, and the full menu (for pictures)
  const [showKitchen, setShowKitchen] = useState(false);
  const [showSoldOut, setShowSoldOut] = useState(false);
  // NEW: the Completed column only shows the latest few until "Show all" is pressed
  const [showAllCompleted, setShowAllCompleted] = useState(false);
  const menu = useMenuItems(isLoggedIn);

  // NEW: the clock moves on by itself, so "5m ago" and warnings stay correct
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 30000);
    return () => clearInterval(timer);
  }, []);

  const navigateToPage = useCallback((page) => {
    if (handlingPopStateRef.current) {
      handlingPopStateRef.current = false;
    } else {
      window.history.pushState({ page }, "", `#${page}`);
    }

    pageRef.current = page;
    setShowSettings(page === "settings");
    setShowAdmin(page === "admin");
  }, []);

  // More Filters
  const [showFilters, setShowFilters] = useState(false);
  const [sortOption, setSortOption] = useState("newest");
  // CHANGED: on by default, so old orders don't pile up on the board
  const [todayOnly, setTodayOnly] = useState(true);

  const handleLogout = useCallback(() => {
    logoutFromServer();

    localStorage.removeItem("token");
    localStorage.removeItem("staffUser");
    localStorage.removeItem("staffRemember");
    sessionStorage.removeItem("staffSessionActive");

    setStaffUser(null);
    setOrders([]);
    setShowManualOrder(false);
    setCancelTarget(null);
    setShowKitchen(false);
    setShowSoldOut(false);

    pageRef.current = "board";
    window.history.replaceState({ page: "board" }, "", "#board");
    setShowAdmin(false);
    setShowSettings(false);
    setIsLoggedIn(false);
  }, []);

  const showNotice = useCallback((type, text) => {
    setNotice({ type, text, id: Date.now() });
  }, []);

  // Messages disappear by themselves after a few seconds
  useEffect(() => {
    if (!notice) return undefined;
    const timer = setTimeout(() => setNotice(null), 5000);
    return () => clearTimeout(timer);
  }, [notice]);

  // PERFORMANCE: never start a reload while one is still running
  const loadingRef = useRef(false);

  const loadOrders = useCallback(
    async (showSpinner = false) => {
      if (loadingRef.current) return;
      loadingRef.current = true;

      if (showSpinner) setLoadingOrders(true);

      try {
        const response = await apiFetch("/api/orders");

        if (response.status === 401) {
          handleLogout();
          return;
        }

        if (response.status === 403) {
          throw new Error("This account is not allowed to view orders.");
        }

        if (!response.ok) {
          throw new Error(`Could not load orders (status ${response.status}).`);
        }

        const data = await response.json();

        const boardOrders = data
          .map(toBoardOrder)
          .filter((order) => order.status)
          .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

        setOrders(boardOrders);
        setOrdersError("");
        setConnected(true);
      } catch (error) {
        console.error("Order loading error:", error);
        setConnected(false);
        setOrdersError(
          error.message === "Failed to fetch"
            ? "Cannot reach the server. Is the backend running?"
            : error.message
        );
      } finally {
        loadingRef.current = false;
        setLoadingOrders(false);
      }
    },
    [handleLogout]
  );

  // NEW: a brand-new order gets a highlight, a sound and a count in the tab title
  const announceNewOrder = useCallback((orderId) => {
    setFreshIds((current) => [...current.filter((id) => id !== orderId), orderId]);
    setTimeout(() => {
      setFreshIds((current) => current.filter((id) => id !== orderId));
    }, NEW_HIGHLIGHT_MS);

    if (document.visibilityState !== "visible" || !document.hasFocus()) {
      setUnseenCount((count) => count + 1);
    }
  }, []);

  // Orders already on the board (so a live message can tell new from updated)
  const knownIdsRef = useRef(new Set());
  useEffect(() => {
    knownIdsRef.current = new Set(orders.map((order) => order.backendId));
  }, [orders]);

  const soundOnRef = useRef(soundOn);
  useEffect(() => {
    soundOnRef.current = soundOn;
  }, [soundOn]);

  // NEW: live updates. The backend sends new orders, status changes and
  // cancellations (by students, staff or the 15-minute auto-cancel) straight here.
  const handleLiveOrder = useCallback(
    (order) => {
      if (!order || !order.orderId) {
        loadOrders(false); // unknown message or reconnected: reload everything
        return;
      }

      const updated = toBoardOrder(order);
      if (!updated.status) return;

      const isNew = !knownIdsRef.current.has(updated.backendId);
      knownIdsRef.current.add(updated.backendId);

      setOrders((current) => {
        const others = current.filter((o) => o.backendId !== updated.backendId);
        return [updated, ...others];
      });

      if (isNew && updated.status === "New") {
        announceNewOrder(updated.id);
        if (soundOnRef.current) playNewOrderSound();
      }
    },
    [loadOrders, announceNewOrder]
  );

  const live = useKitchenUpdates(isLoggedIn, handleLiveOrder);

  // First load when the board opens (straight after it appears, not during it)
  useEffect(() => {
    if (!isLoggedIn || showSettings || showAdmin) return undefined;
    const first = setTimeout(() => loadOrders(true), 0);
    return () => clearTimeout(first);
  }, [isLoggedIn, showSettings, showAdmin, loadOrders]);

  // PERFORMANCE: backup reload only while the tab is visible,
  // and straight away when the staff member comes back to the tab
  useEffect(() => {
    if (!isLoggedIn || showSettings || showAdmin) return undefined;

    const timer = setInterval(
      () => {
        if (document.visibilityState === "visible") loadOrders(false);
      },
      live ? REFRESH_LIVE_MS : REFRESH_OFFLINE_MS
    );

    const onVisible = () => {
      if (document.visibilityState === "visible") loadOrders(false);
    };

    document.addEventListener("visibilitychange", onVisible);

    return () => {
      clearInterval(timer);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [isLoggedIn, showSettings, showAdmin, loadOrders, live]);

  // NEW: "(2) New orders" in the browser tab until staff look at the board again
  useEffect(() => {
    document.title =
      unseenCount > 0
        ? `(${unseenCount}) New order${unseenCount > 1 ? "s" : ""} · EduEats staff`
        : "EduEats staff";
  }, [unseenCount]);

  useEffect(() => {
    const seen = () => {
      if (document.visibilityState === "visible") setUnseenCount(0);
    };
    window.addEventListener("focus", seen);
    document.addEventListener("visibilitychange", seen);
    return () => {
      window.removeEventListener("focus", seen);
      document.removeEventListener("visibilitychange", seen);
    };
  }, []);

  // PERFORMANCE: only re-filter when the orders or filters change
  const filteredOrders = useMemo(() => {
    const searchText = search.trim().toLowerCase();

    return orders
      .filter((order) => {
        const matchesSearch = !searchText || order.searchText.includes(searchText);

        const matchesFilter =
          activeFilter === "All Orders" ||
          (activeFilter === "Cancelled" && order.cancelled) ||
          (activeFilter === "Completed" && order.status === "Completed" && !order.cancelled) ||
          (activeFilter !== "Completed" && order.status === activeFilter);

        const matchesToday = !todayOnly || isToday(order.createdAt, now);

        return matchesSearch && matchesFilter && matchesToday;
      })
      .sort((a, b) => {
        const aTime = new Date(a.createdAt).getTime();
        const bTime = new Date(b.createdAt).getTime();

        switch (sortOption) {
          case "oldest":
            return aTime - bTime;
          case "highest-price":
            return b.total - a.total;
          case "lowest-price":
            return a.total - b.total;
          case "newest":
          default:
            return bTime - aTime;
        }
      });
  }, [orders, search, activeFilter, todayOnly, sortOption, now]);

  const roleName = String(staffUser?.role?.roleName || staffUser?.role || "").toUpperCase();

  const isAdmin = roleName === "ADMIN";

  useEffect(() => {
    const onBackOrForward = (event) => {
      const requestedPage =
        event.state?.page || window.location.hash.replace(/^#/, "") || "board";

      const allowedPage =
        isLoggedIn &&
        (requestedPage === "board" ||
          requestedPage === "settings" ||
          (requestedPage === "admin" && isAdmin))
          ? requestedPage
          : "board";

      handlingPopStateRef.current = true;
      navigateToPage(allowedPage);

      if (allowedPage !== requestedPage) {
        window.history.replaceState({ page: allowedPage }, "", `#${allowedPage}`);
      }
    };

    window.addEventListener("popstate", onBackOrForward);

    return () => {
      window.removeEventListener("popstate", onBackOrForward);
    };
  }, [isLoggedIn, isAdmin, navigateToPage]);

  if (!isLoggedIn) {
    return (
      <Login
        onLogin={(user) => {
          setStaffUser(user);
          setIsLoggedIn(true);
        }}
      />
    );
  }

  const staffName = staffUser?.firstName || "Staff";
  const connection = !connected ? "offline" : live ? "live" : "online";
  const currentPage = showSettings ? "settings" : "board";

  // CHANGED: the same top bar (with page links) on every page
  const header = (
    <StaffHeader
      page={currentPage}
      isAdmin={isAdmin}
      connection={connection}
      staffName={staffName}
      fullName={`${staffUser?.firstName || ""} ${staffUser?.lastName || ""}`.trim() || staffName}
      onNavigate={navigateToPage}
      onLogout={handleLogout}
    />
  );

  // CHANGED: Settings is now a real account page (edit details, password,
  // sound, deactivate). Logging out is done from the top bar.
  if (showSettings) {
    return (
      <div className="app">
        {header}

        <AccountSettings
          key={staffUser?.email}
          staffUser={staffUser}
          roleName={roleName}
          soundOn={soundOn}
          onSoundChange={(on) => {
            setSoundOn(on);
            saveSoundOn(on);
            if (on) playNewOrderSound();
          }}
          onBack={() => navigateToPage("board")}
          onUserUpdated={(changes) => {
            const updated = { ...staffUser, ...changes };
            setStaffUser(updated);
            try {
              localStorage.setItem("staffUser", JSON.stringify(updated));
            } catch {
              // not saved - the new name shows again after the next login
            }
          }}
          onLogout={handleLogout}
        />
      </div>
    );
  }

  if (showAdmin && isAdmin) {
    return (
      <AdminDashboard
        staffUser={staffUser}
        onBack={() => navigateToPage("board")}
        onLogout={handleLogout}
      />
    );
  }

  // PERFORMANCE: the card moves straight away (optimistic update).
  // The backend is updated in the background; if it fails, the card moves back.
  const updateOrderStatus = async (orderId, newStatus) => {
    const order = orders.find((o) => o.id === orderId);

    if (!order || !newStatus) return;

    const steps = getStatusSteps(order.backendStatus, newStatus);
    const previous = {
      status: order.status,
      backendStatus: order.backendStatus,
      statusKey: order.statusKey,
    };

    const setOrder = (changes) =>
      setOrders((current) => current.map((o) => (o.id === orderId ? { ...o, ...changes } : o)));

    const optimistic = OPTIMISTIC_STATUS[newStatus] || order.backendStatus;
    setOrder({ status: newStatus, backendStatus: optimistic, statusKey: optimistic.toLowerCase() });
    setUpdatingId(orderId);

    try {
      let latest = null;

      for (const statusName of steps) {
        const response = await apiFetch(`/api/orders/${order.backendId}/status`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ statusName }),
        });

        if (response.status === 401) {
          handleLogout();
          return;
        }

        if (!response.ok) {
          throw new Error(await readError(response, `Could not change ${orderId} to ${statusName}.`));
        }

        try {
          latest = await response.json();
        } catch {
          latest = null;
        }
      }

      // Use the order the backend sent back instead of reloading every order
      if (latest && latest.orderId) {
        const updated = toBoardOrder(latest);
        setOrders((current) =>
          current
            .map((o) => (o.backendId === updated.backendId ? updated : o))
            .filter((o) => o.status)
        );
      }
    } catch (error) {
      console.error("Status update error:", error);
      setOrder(previous); // move the card back
      showNotice("error", error.message);
    } finally {
      setUpdatingId(null);
    }
  };

  // NEW: staff can cancel an order that hasn't been started yet
  // (e.g. an item ran out). The student is told straight away.
  const cancelOrder = async (order) => {
    setCancelTarget(null);
    setUpdatingId(order.id);

    try {
      const response = await apiFetch(`/api/orders/${order.backendId}/cancel`, { method: "PUT" });

      if (response.status === 401) {
        handleLogout();
        return;
      }

      if (!response.ok) {
        throw new Error(await readError(response, `Could not cancel ${order.id}.`));
      }

      setOrders((current) =>
        current.map((o) =>
          o.backendId === order.backendId
            ? { ...o, status: "Completed", cancelled: true, backendStatus: "Cancelled", statusKey: "cancelled" }
            : o
        )
      );
      showNotice("success", `${order.id} was cancelled. The customer has been told.`);
    } catch (error) {
      console.error("Cancel error:", error);
      showNotice("error", error.message);
      loadOrders(false);
    } finally {
      setUpdatingId(null);
    }
  };

  const handleManualOrderPlaced = (order) => {
    setShowManualOrder(false);
    const boardOrder = toBoardOrder(order);
    knownIdsRef.current.add(boardOrder.backendId);
    setOrders((current) => [
      boardOrder,
      ...current.filter((o) => o.backendId !== boardOrder.backendId),
    ]);
    announceNewOrder(boardOrder.id);
    showNotice("success", `Manual order ${boardOrder.id} placed for ${boardOrder.customer}.`);
  };

  // NEW: how long a "Submitted" order has waited, and the auto-cancel warning
  const waitingInfo = (order) => {
    if (order.statusKey !== "submitted") return null;
    const waited = minutesSince(order.createdAt, now);
    const left = AUTO_CANCEL_MINUTES - waited;
    if (left > WARN_BEFORE_CANCEL_MINUTES) return null;
    return left > 0
      ? `Not accepted yet · auto-cancels in ${left} min`
      : "Not accepted in time · cancelling…";
  };

  const renderLines = (order) => (
    <div className="order-items">
      {order.lines.map((line, index) => (
        <div className="order-line" key={index}>
          <ItemThumb item={menu.findByName(line.name) || { name: line.name }} />
          <div className="order-line-text">
            <div>
              {line.quantity}x {line.name}
            </div>
            {line.extras.length > 0 && <small className="order-extras">{line.extras.join(", ")}</small>}
            {line.note && <small className="order-note">Note: {line.note}</small>}
          </div>
        </div>
      ))}
    </div>
  );

  const soldOutCount = menu.items.filter((item) => item.available === false).length;

  return (
    <div className="app">
      {header}

      {notice && (
        <div className={`staff-notice ${notice.type}`} role="status" key={notice.id}>
          <span>{notice.text}</span>
          <button type="button" onClick={() => setNotice(null)} aria-label="Close message">
            ×
          </button>
        </div>
      )}

      {/* MAIN CONTENT */}
      <main className="dashboard">
        {/* PAGE TOOLBAR */}
        <section className="toolbar">
          <div className="toolbar-left">
            <div className="search-box">
              <span>⌕</span>

              <input
                type="text"
                placeholder="Search orders, customers, items..."
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
            </div>

            <select
              className="filter-select"
              value={activeFilter}
              onChange={(event) => setActiveFilter(event.target.value)}
              aria-label="Show orders"
            >
              {FILTERS.map((filter) => (
                <option key={filter}>{filter}</option>
              ))}
            </select>
          </div>

          <div className="toolbar-right">
            <div className="view-toggle">
              <button className={view === "board" ? "selected" : ""} onClick={() => setView("board")}>
                Board
              </button>

              <button className={view === "list" ? "selected" : ""} onClick={() => setView("list")}>
                List
              </button>
            </div>

            <button
              type="button"
              className={`secondary-button sound-toggle ${soundOn ? "on" : ""}`}
              onClick={() => {
                setSoundOn(!soundOn);
                saveSoundOn(!soundOn);
                if (!soundOn) playNewOrderSound();
              }}
              aria-pressed={soundOn}
              title={soundOn ? "New-order sound is on" : "New-order sound is off"}
            >
              {soundOn ? "🔔" : "🔕"} <span>Sound</span>
            </button>

            <button
              className="secondary-button"
              onClick={() => loadOrders(true)}
              disabled={loadingOrders}
            >
              {loadingOrders ? "Refreshing..." : "Refresh"}
            </button>

            {/* MORE FILTERS */}
            <div className="more-filters-wrapper">
              <button
                className={`secondary-button ${!todayOnly || sortOption !== "newest" ? "filters-changed" : ""}`}
                onClick={() => setShowFilters((current) => !current)}
                aria-expanded={showFilters}
              >
                More Filters
              </button>

              {showFilters && (
                <div className="more-filters-panel">
                  <div className="more-filters-title">Sort by</div>

                  {[
                    ["newest", "Newest"],
                    ["oldest", "Oldest"],
                    ["highest-price", "Highest price"],
                    ["lowest-price", "Lowest price"],
                  ].map(([value, label]) => (
                    <label key={value}>
                      <input
                        type="radio"
                        name="order-sort"
                        value={value}
                        checked={sortOption === value}
                        onChange={(event) => setSortOption(event.target.value)}
                      />
                      {label}
                    </label>
                  ))}

                  <div className="more-filters-divider" />

                  <label>
                    <input
                      type="checkbox"
                      checked={todayOnly}
                      onChange={(event) => setTodayOnly(event.target.checked)}
                    />
                    Today only
                  </label>

                  <button
                    className="filter-reset"
                    onClick={() => {
                      setSortOption("newest");
                      setTodayOnly(true);
                    }}
                  >
                    Reset Filters
                  </button>
                </div>
              )}
            </div>

            <button
              type="button"
              className={`secondary-button ${soldOutCount ? "has-sold-out" : ""}`}
              onClick={() => setShowSoldOut(true)}
            >
              Sold out{soldOutCount ? ` (${soldOutCount})` : ""}
            </button>

            <button type="button" className="secondary-button" onClick={() => setShowKitchen(true)}>
              ⛶ Kitchen view
            </button>

            <button className="primary-button" onClick={() => setShowManualOrder(true)}>
              + New Manual Order
            </button>
          </div>
        </section>

        {/* DASHBOARD TITLE */}
        <section className="dashboard-heading">
          <div>
            <h1>Order Management</h1>
            <p>
              {todayOnly ? "Today's orders." : "All orders."}{" "}
              {live
                ? "New orders appear here automatically."
                : "Checking for new orders every 15 seconds."}
            </p>
          </div>

          <div className="order-count">{filteredOrders.length} orders</div>
        </section>

        {/* NEW: today at a glance */}
        <StatsBar orders={orders} now={now} />

        {ordersError && <div className="board-error">{ordersError}</div>}

        {/* KANBAN BOARD */}
        {view === "board" ? (
          <section className="kanban-board">
            {STATUSES.map((status) => {
              const columnOrders = filteredOrders.filter((order) => order.status === status);

              // Keep the board tidy on a busy day: only the latest finished orders
              const hiddenCount =
                status === "Completed" && !showAllCompleted
                  ? Math.max(0, columnOrders.length - COMPLETED_SHOWN)
                  : 0;
              const shownOrders = hiddenCount ? columnOrders.slice(0, COMPLETED_SHOWN) : columnOrders;

              return (
                <div className="kanban-column" key={status}>
                  <div className="column-heading">
                    <div className="column-title">
                      <span className={`status-dot ${status.toLowerCase().replace(" ", "-")}`} />

                      <h2>{status}</h2>
                    </div>

                    <span className="column-count">{columnOrders.length}</span>
                  </div>

                  <div className="order-list">
                    {shownOrders.map((order) => {
                      const warning = waitingInfo(order);
                      const isFresh = freshIds.includes(order.id);

                      return (
                        <article
                          className={`order-card ${isFresh ? "is-fresh" : ""} ${
                            order.cancelled ? "is-cancelled" : ""
                          } ${warning ? "is-waiting" : ""}`}
                          key={order.id}
                          style={updatingId === order.id ? { opacity: 0.6 } : undefined}
                        >
                          <div className="order-card-top">
                            <strong>{order.id}</strong>

                            {isFresh ? (
                              <span className="fresh-badge">NEW</span>
                            ) : (
                              <span className="order-time" title={new Date(order.createdAt).toLocaleString("en-ZA")}>
                                {clockTime(order.createdAt, now)}
                              </span>
                            )}
                          </div>

                          <div className="order-customer">
                            {order.customer}
                            {order.walkIn && <span className="walk-in-tag">Walk-in</span>}
                          </div>

                          {renderLines(order)}

                          {warning && <p className="waiting-warning">⏱ {warning}</p>}

                          <div className="order-card-bottom">
                            <span className="order-time">◷ {timeAgo(order.createdAt, now)}</span>

                            <strong className="order-price">{formatPrice(order.total)}</strong>
                          </div>

                          {order.cancelled ? (
                            <p className="cancelled-label">Cancelled</p>
                          ) : (
                            <div className="order-actions">
                              {status === "New" && (
                                <button
                                  className="cancel-order"
                                  disabled={updatingId === order.id}
                                  onClick={() => setCancelTarget(order)}
                                >
                                  Cancel
                                </button>
                              )}

                              {status !== "New" && (
                                <button
                                  disabled={updatingId === order.id}
                                  onClick={() =>
                                    updateOrderStatus(order.id, STATUSES[STATUSES.indexOf(status) - 1])
                                  }
                                >
                                  ← Back
                                </button>
                              )}

                              {status !== "Completed" && (
                                <button
                                  className="next-status"
                                  disabled={updatingId === order.id}
                                  onClick={() =>
                                    updateOrderStatus(order.id, STATUSES[STATUSES.indexOf(status) + 1])
                                  }
                                >
                                  {NEXT_LABEL[status]}
                                </button>
                              )}
                            </div>
                          )}
                        </article>
                      );
                    })}

                    {hiddenCount > 0 && (
                      <button
                        type="button"
                        className="show-more-orders"
                        onClick={() => setShowAllCompleted(true)}
                      >
                        Show all {columnOrders.length} finished orders
                      </button>
                    )}

                    {status === "Completed" && showAllCompleted && columnOrders.length > COMPLETED_SHOWN && (
                      <button
                        type="button"
                        className="show-more-orders"
                        onClick={() => setShowAllCompleted(false)}
                      >
                        Show only the latest {COMPLETED_SHOWN}
                      </button>
                    )}

                    {columnOrders.length === 0 && (
                      <div className="empty-column">{loadingOrders ? "Loading..." : "No orders"}</div>
                    )}
                  </div>
                </div>
              );
            })}
          </section>
        ) : (
          /* LIST VIEW */
          <section className="list-view">
            <div className="list-header">
              <span>Order</span>
              <span>Customer</span>
              <span>Items</span>
              <span>Status</span>
              <span>Time</span>
              <span>Total</span>
            </div>

            {filteredOrders.map((order) => (
              <div className="list-row" key={order.id}>
                <strong>{order.id}</strong>
                <span>
                  {order.customer}
                  {order.walkIn && <span className="walk-in-tag">Walk-in</span>}
                </span>
                <span>{itemsSummary(order)}</span>

                <span>
                  <span className={`status-badge ${order.cancelled ? "cancelled" : ""}`}>
                    {order.cancelled ? "Cancelled" : order.status}
                  </span>
                </span>

                <span>
                  {clockTime(order.createdAt, now)}
                  <small className="list-ago"> · {timeAgo(order.createdAt, now)}</small>
                </span>
                <strong>{formatPrice(order.total)}</strong>
              </div>
            ))}

            {filteredOrders.length === 0 && (
              <div className="empty-column list-empty">
                {loadingOrders ? "Loading..." : "No orders match these filters."}
              </div>
            )}
          </section>
        )}
      </main>

      {showManualOrder && (
        <ManualOrder
          staffUser={staffUser}
          onClose={() => setShowManualOrder(false)}
          onPlaced={handleManualOrderPlaced}
          onSessionExpired={handleLogout}
        />
      )}

      {showKitchen && (
        <KitchenView
          orders={orders}
          now={now}
          live={live}
          updatingId={updatingId}
          findItem={menu.findByName}
          onAdvance={updateOrderStatus}
          onExit={() => setShowKitchen(false)}
        />
      )}

      {showSoldOut && (
        <SoldOutPanel
          items={menu.items}
          isAdmin={isAdmin}
          onChanged={(changed) => {
            menu.setItems((current) =>
              current.map((item) => (item.menuItemId === changed.menuItemId ? changed : item))
            );
            showNotice(
              "success",
              changed.available
                ? `${changed.name} is back on the student menu.`
                : `${changed.name} is marked sold out and hidden from students.`
            );
          }}
          onSessionExpired={handleLogout}
          onClose={() => setShowSoldOut(false)}
        />
      )}

      {cancelTarget && (
        <ConfirmDialog
          danger
          title={`Cancel ${cancelTarget.id}?`}
          message={`${cancelTarget.customer}'s order will be cancelled and they'll be told straight away. Use this if an item ran out or the order can't be made.`}
          confirmLabel="Yes, cancel"
          cancelLabel="Keep order"
          onConfirm={() => cancelOrder(cancelTarget)}
          onCancel={() => setCancelTarget(null)}
        />
      )}
    </div>
  );
}

export default App;
