import { useCallback, useEffect, useRef, useState } from "react";
import Login from "./pages/Login/Login";
import Register from "./pages/Register/Register";
import Menu from "./pages/Menu/Menu";
import Orders from "./pages/Orders/Orders";
import Profile from "./pages/Profile/Profile";
import IdleWarning from "./components/IdleWarning";
import OrderAlertBanner from "./components/OrderAlertBanner";
import { apiFetch, logoutFromServer, SESSION_MISMATCH_EVENT } from "./api";
import useIdleLogout, { clearIdleClock, resetIdleClock } from "./useIdleLogout";
import useOrderUpdates from "./useOrderUpdates";
import { isCancelled, isFinished, isReady, wasCancelledByStudent } from "./orderStatus";
import { showCancelledNotification, showReadyNotification } from "./readyNotifications";

// Pages you can only see when logged in, and pages for logged-out users
const PRIVATE_PAGES = ["menu", "cart", "orders", "profile"];
const PUBLIC_PAGES = ["login", "register"];

// The cart is kept for this browser tab, so a refresh doesn't empty it
const CART_KEY = "edueatsCart";
// How many items were in the cart the last time the student opened it
const CART_SEEN_KEY = "edueatsCartSeen";
// Orders placed since the student last opened My Orders
const NEW_ORDERS_KEY = "edueatsNewOrders";

const SESSION_MISMATCH_MESSAGE =
  "You were signed out because another account (e.g. the staff app) signed in on this browser. Please sign in again.";

const IDLE_LOGOUT_MESSAGE =
  "You were logged out after 15 minutes without activity, to keep your account safe.";

// Reads the page from the address bar, e.g. "#orders" -> "orders"
function pageFromAddress() {
  const page = window.location.hash.replace(/^#\/?/, "");
  return [...PRIVATE_PAGES, ...PUBLIC_PAGES].includes(page) ? page : null;
}

// Stops logged-out users reaching private pages (and the other way round)
function allowedPage(page, loggedIn) {
  if (loggedIn) return PRIVATE_PAGES.includes(page) ? page : "menu";
  return PUBLIC_PAGES.includes(page) ? page : "login";
}

function loadSavedUser() {
  try {
    return JSON.parse(localStorage.getItem("user"));
  } catch {
    return null;
  }
}

function loadSavedNumber(key) {
  try {
    return Number(sessionStorage.getItem(key)) || 0;
  } catch {
    return 0;
  }
}

// Tells the page animation which way to slide ("forward" or "back")
function setNavDirection(direction) {
  document.documentElement.dataset.nav = direction;
}

function loadSavedCart() {
  try {
    const saved = JSON.parse(sessionStorage.getItem(CART_KEY));
    return Array.isArray(saved) ? saved : [];
  } catch {
    return [];
  }
}

function App() {
  const [user, setUser] = useState(loadSavedUser);
  const [token, setToken] = useState(() => localStorage.getItem("token") || "");
  const loggedIn = Boolean(user && token);

  const [page, setPage] = useState(() =>
    allowedPage(pageFromAddress() || "menu", loggedIn)
  );

  // A short message shown on the login page, e.g. after deleting the account
  const [loginMessage, setLoginMessage] = useState("");

  useEffect(() => {
    if (!loginMessage) return undefined;
    const timer = setTimeout(() => setLoginMessage(""), 7000);
    return () => clearTimeout(timer);
  }, [loginMessage]);

  // NEW: the cart lives here, so it isn't lost when you open My Orders
  const [cart, setCart] = useState(loadSavedCart);

  useEffect(() => {
    try {
      sessionStorage.setItem(CART_KEY, JSON.stringify(cart));
    } catch {
      // storage not available - the cart still works, it just isn't saved
    }
  }, [cart]);

  const cartCount = cart.reduce((total, item) => total + item.quantity, 0);

  // NEW: the bubble on the ☰ button only shows NEW items.
  // Opening the cart "sees" them, so the bubble clears.
  const [cartSeen, setCartSeen] = useState(() => loadSavedNumber(CART_SEEN_KEY));

  // On the cart page, or items were removed: everything counts as seen.
  // (Adjusted while rendering, the React-recommended way, instead of in an effect.)
  const seenNow = page === "cart" ? cartCount : Math.min(cartSeen, cartCount);
  if (seenNow !== cartSeen) {
    setCartSeen(seenNow);
  }

  useEffect(() => {
    try {
      sessionStorage.setItem(CART_SEEN_KEY, String(cartSeen));
    } catch {
      // not saved - only means the bubble may show again after a refresh
    }
  }, [cartSeen]);

  const cartAlert = cartCount > cartSeen;

  // NEW: green "1 new" on My Orders after placing an order.
  // Opening My Orders clears it (adjusted while rendering, like cartSeen).
  const [newOrderCount, setNewOrderCount] = useState(() =>
    loadSavedNumber(NEW_ORDERS_KEY)
  );
  if (page === "orders" && newOrderCount !== 0) {
    setNewOrderCount(0);
  }

  useEffect(() => {
    try {
      sessionStorage.setItem(NEW_ORDERS_KEY, String(newOrderCount));
    } catch {
      // not saved - the badge may not show again after a refresh
    }
  }, [newOrderCount]);

  const handleOrderPlaced = () => setNewOrderCount((count) => count + 1);

  // Opening the ☰ side menu also counts as "seen" (the cart count is shown there)
  const markCartSeen = () => setCartSeen(cartCount);

  // Refs so the Back-button listener always sees the latest values
  const pageRef = useRef(page);
  const loggedInRef = useRef(loggedIn);

  useEffect(() => {
    loggedInRef.current = loggedIn;
  }, [loggedIn]);

  // NEW: go to a page AND add it to the browser history,
  // so the phone/browser Back button goes to the previous page
  const navigate = useCallback((next, { replace = false } = {}) => {
    if (!replace && pageRef.current === next) return;

    setNavDirection("forward");
    const method = replace ? "replaceState" : "pushState";
    window.history[method]({ page: next }, "", `#${next}`);

    pageRef.current = next;
    setPage(next);
    window.scrollTo({ top: 0 });
  }, []);

  // NEW: listen for the Back/Forward buttons
  useEffect(() => {
    // Mark the first page in the history, so Back has somewhere to land
    window.history.replaceState({ page: pageRef.current }, "", `#${pageRef.current}`);

    const onBackOrForward = (event) => {
      setNavDirection("back"); // slide the page in from the left
      const wanted = event.state?.page || pageFromAddress() || "menu";
      const allowed = allowedPage(wanted, loggedInRef.current);

      // e.g. pressing Back after logging out must not show the menu again
      if (allowed !== wanted) {
        window.history.replaceState({ page: allowed }, "", `#${allowed}`);
      }

      pageRef.current = allowed;
      setPage(allowed);
      window.scrollTo({ top: 0 });
    };

    window.addEventListener("popstate", onBackOrForward);
    return () => window.removeEventListener("popstate", onBackOrForward);
  }, []);

  const handleLoginSuccess = (loggedInUser, authToken) => {
    resetIdleClock(); // start the 15-minute idle timer fresh
    setLoginMessage("");
    setUser(loggedInUser);
    setToken(authToken);
    loggedInRef.current = true;

    // replace = Back won't take a logged-in student back to the login page
    navigate("menu", { replace: true });
  };

  // How long the "logging out" fade takes before the login page appears
  const LOGOUT_ANIMATION_MS = 260;
  const loggingOutRef = useRef(false);

  // keepServerSession: true when the login cookie belongs to ANOTHER
  // account (e.g. staff) - then we must not cancel it on the server
  const finishLogout = (message = "", keepServerSession = false) => {
    // Tell the backend to cancel the tokens (runs in the background,
    // the student is logged out on the frontend straight away)
    if (!keepServerSession) logoutFromServer();

    localStorage.removeItem("token");
    localStorage.removeItem("user");
    clearIdleClock();
    setCart([]);
    setCartSeen(0);
    setNewOrderCount(0);

    setUser(null);
    setToken("");
    loggedInRef.current = false;
    navigate("login", { replace: true });
    setLoginMessage(message);

    // The login card gets its own "welcome back" zoom-in instead of a slide
    setNavDirection("logout");
    document.body.classList.remove("app-leaving");
    loggingOutRef.current = false;
  };

  // NEW: the current page fades out first, then the login page zooms in
  // message (optional): shown on the login page afterwards
  const handleLogout = (message, keepServerSession = false) => {
    if (loggingOutRef.current) return; // already logging out
    loggingOutRef.current = true;

    const text = typeof message === "string" ? message : "";
    const reduceMotion = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
    if (reduceMotion) {
      finishLogout(text, keepServerSession);
      return;
    }

    document.body.classList.add("app-leaving");
    setTimeout(() => finishLogout(text, keepServerSession), LOGOUT_ANIMATION_MS);
  };

  // The backend tried to swap this student's login for another account's
  // (shared cookie on localhost). Sign out instead of ordering as them.
  const handleLogoutRef = useRef(handleLogout);
  useEffect(() => {
    handleLogoutRef.current = handleLogout;
  });

  useEffect(() => {
    const onMismatch = () => handleLogoutRef.current(SESSION_MISMATCH_MESSAGE, true);
    window.addEventListener(SESSION_MISMATCH_EVENT, onMismatch);
    return () => window.removeEventListener(SESSION_MISMATCH_EVENT, onMismatch);
  }, []);

  // Asked by the idle timer: is food being made for this student right now?
  const userId = user?.userId;
  const hasOrderInProgress = useCallback(async () => {
    if (!userId) return false;
    try {
      const response = await apiFetch(`/api/orders/user/${userId}`);
      if (!response.ok) return false;
      const orders = await response.json();
      return Array.isArray(orders) && orders.some((order) => !isFinished(order.status));
    } catch {
      return false;
    }
  }, [userId]);

  // NEW: log out after 15 minutes without activity (warning for the last minute).
  // Not while an order is in progress - the student may be waiting for food.
  const idleSecondsLeft = useIdleLogout(
    loggedIn,
    () => handleLogout(IDLE_LOGOUT_MESSAGE),
    hasOrderInProgress
  );
  // NEW: order news anywhere in the app, not only on My Orders:
  // "Your order is ready!" and "Your order was cancelled" (e.g. the
  // cafeteria didn't accept it in time). Uses the same live updates.
  // - App in the background: a phone/laptop notification (if allowed)
  // - Another page open: a bar with a "View" button
  const [orderAlert, setOrderAlert] = useState(null); // { kind, order }
  const alertedRef = useRef(new Set()); // each order + news alerts only once

  const handleLiveOrderUpdate = useCallback((order) => {
    if (!order) return;

    let kind = null;
    if (isReady(order.status)) kind = "ready";
    // Cancelled by the student on this phone: they already know
    else if (isCancelled(order.status) && !wasCancelledByStudent(order.orderId)) kind = "cancelled";
    if (!kind) return;

    const key = `${kind}-${order.orderId}`;
    if (alertedRef.current.has(key)) return;
    alertedRef.current.add(key);

    if (document.visibilityState === "hidden") {
      if (kind === "ready") showReadyNotification(order);
      else showCancelledNotification(order);
    }
    if (pageRef.current !== "orders") {
      setOrderAlert({ kind, order });
      if (navigator.vibrate) navigator.vibrate([200, 100, 200]);
    }
  }, []);

  useOrderUpdates(loggedIn ? userId : null, handleLiveOrderUpdate);

  // Opening My Orders shows the same news there, so the bar can go.
  // Logged out: no bar either.
  if ((page === "orders" || !loggedIn) && orderAlert) {
    setOrderAlert(null);
  }

  const orderAlertBanner = orderAlert && (
    <OrderAlertBanner
      alert={orderAlert}
      onView={() => {
        setOrderAlert(null);
        navigate("orders");
      }}
      onClose={() => setOrderAlert(null)}
    />
  );

  const idleWarning = idleSecondsLeft !== null && (
    <IdleWarning secondsLeft={idleSecondsLeft} onLogout={() => handleLogout()} />
  );

  // My Profile saved new details: keep the name in the header up to date
  const handleUserUpdated = (updatedUser) => {
    const merged = { ...user, ...updatedUser };
    setUser(merged);
    try {
      localStorage.setItem("user", JSON.stringify(merged));
    } catch {
      // not saved - the new name shows again after the next login
    }
  };

  // key={page}: Login and Register get the same slide-in as the other pages
  if (page === "register") {
    return (
      <div key={page} className="page-enter">
        <Register
          onGoToLogin={(message) => {
            setLoginMessage(typeof message === "string" ? message : "");
            navigate("login");
          }}
        />
      </div>
    );
  }

  if (page === "login") {
    return (
      <div key={page} className="page-enter">
        {loginMessage && (
          <div className="login-flash" role="status">
            <span>{loginMessage}</span>
            <button type="button" onClick={() => setLoginMessage("")} aria-label="Close message">
              ×
            </button>
          </div>
        )}
        <Login
          onGoToRegister={() => navigate("register")}
          onLoginSuccess={handleLoginSuccess}
        />
      </div>
    );
  }

  if (page === "orders") {
    return (
      <>
        <Orders
          user={user}
          token={token}
          cartCount={cartCount}
          cartAlert={cartAlert}
          onCartSeen={markCartSeen}
          onNavigate={navigate}
          onLogout={handleLogout}
        />
        {idleWarning}
      </>
    );
  }

  if (page === "profile") {
    return (
      <>
        <Profile
          user={user}
          cartCount={cartCount}
          cartAlert={cartAlert}
          onCartSeen={markCartSeen}
          newOrderCount={newOrderCount}
          onNavigate={navigate}
          onLogout={handleLogout}
          onUserUpdated={handleUserUpdated}
        />
        {orderAlertBanner}
        {idleWarning}
      </>
    );
  }

  // "menu" and "cart" are both handled by the Menu page
  return (
    <>
      <Menu
        user={user}
        token={token}
        view={page === "cart" ? "cart" : "menu"}
        cart={cart}
        setCart={setCart}
        cartAlert={cartAlert}
        onCartSeen={markCartSeen}
        newOrderCount={newOrderCount}
        onOrderPlaced={handleOrderPlaced}
        onNavigate={navigate}
        onLogout={handleLogout}
      />
      {orderAlertBanner}
      {idleWarning}
    </>
  );
}

export default App;
