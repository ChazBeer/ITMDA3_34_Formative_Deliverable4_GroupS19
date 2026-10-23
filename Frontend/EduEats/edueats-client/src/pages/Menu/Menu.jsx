import { useEffect, useMemo, useRef, useState } from "react";
import "./Menu.css";
import { errorFromBody } from "../../apiError";
import { apiFetch } from "../../api";
import useCafeteriaStatus from "../../useCafeteriaStatus";
import { orderLineTotal } from "../../orderLine";
import AppHeader from "../../components/AppHeader";
import CategoryBar from "../../components/CategoryBar";
import CustomiseSheet from "../../components/CustomiseSheet";
import NotifyPrompt from "../../components/NotifyPrompt";
import {
  normalizeGroups,
  makeLineId,
  lineIdOf,
  unitPrice,
  describeModifiers,
} from "./customizations";
import {
  matchesSearch,
  getCategoryName,
  getImageUrl,
  getPlaceholderIcon,
} from "./menuUtils";

// Most of one item a student can order at once
const MAX_QTY_PER_ITEM = 10;

const SORT_OPTIONS = [
  { value: "default", label: "Recommended" },
  { value: "price-asc", label: "Price: low to high" },
  { value: "price-desc", label: "Price: high to low" },
  { value: "name-asc", label: "Name: A to Z" },
];

const formatRand = (amount) => `R${Number(amount ?? 0).toFixed(2)}`;

// NEW: stock. How many of an item the kitchen still has,
// or null if the backend doesn't send a stock number (no limit then).
const LOW_STOCK = 5; // "Only 3 left" from this number down
const stockOf = (item) => {
  if (item?.stockQuantity === undefined || item?.stockQuantity === null) return null;
  const count = Number(item.stockQuantity);
  return Number.isFinite(count) ? Math.max(0, count) : null;
};

// Checkout refused because something sold out / not enough left
const isStockError = (status, message) =>
  status === 503 || /stock|unavailable/i.test(message || "");

// NEW: picture of the item, or a placeholder until images are added
function ItemImage({ item, categoryName }) {
  const [failed, setFailed] = useState(false);
  const url = getImageUrl(item);

  if (url && !failed) {
    return (
      <div className="menu-card-image">
        <img
          src={url}
          alt={item.name}
          loading="lazy"
          onError={() => setFailed(true)}
        />
      </div>
    );
  }

  return (
    <div className="menu-card-image placeholder" aria-hidden="true">
      <span>{getPlaceholderIcon(categoryName)}</span>
    </div>
  );
}

// How long the green "Open" banner stays before it tucks away to the side
const BANNER_VISIBLE_MS = 4000;

// Open/closed + connection banner.
// Green "Open": shows for a few seconds, then slides away into a small pill
// on the side (tap the pill to see the hours again).
// Red "Closed" and grey "Offline" always stay, because students must see them.
// Remembers that the green banner already slid away once,
// so it doesn't pop back up every time the student switches page
let bannerCollapsedBefore = false;

function StatusBanner({ status }) {
  // "full" -> "leaving" (slide-out animation) -> "pill"
  const [phase, setPhase] = useState(() => (bannerCollapsedBefore ? "pill" : "full"));

  const isOpenAndOnline =
    status.loaded && status.connected && status.isOpen === true;

  // Start the timer whenever the full green banner is showing
  useEffect(() => {
    if (!isOpenAndOnline || phase !== "full") return undefined;
    const timer = setTimeout(() => setPhase("leaving"), BANNER_VISIBLE_MS);
    return () => clearTimeout(timer);
  }, [isOpenAndOnline, phase]);

  // After the slide-out animation, show the small pill
  useEffect(() => {
    if (phase !== "leaving") return undefined;
    const timer = setTimeout(() => {
      bannerCollapsedBefore = true;
      setPhase("pill");
    }, 350);
    return () => clearTimeout(timer);
  }, [phase]);

  if (!status.loaded) return null;

  if (!status.connected) {
    return (
      <div className="status-banner offline" role="status">
        <span className="status-dot" />
        Offline: can't reach the cafeteria right now. Check your connection.
      </div>
    );
  }

  // Opening hours unknown: don't show a banner
  if (status.isOpen === null) return null;

  if (status.isOpen) {
    const hours = status.closingTime ? `Open until ${status.closingTime}` : "Open now";

    if (phase === "pill") {
      return (
        <button
          type="button"
          className="status-pill"
          onClick={() => setPhase("full")}
          aria-label={`${hours}. Tap for details`}
          title={hours}
        >
          <span className="status-dot" />
          Open
        </button>
      );
    }

    return (
      <div
        className={`status-banner open ${phase === "leaving" ? "leaving" : ""}`}
        role="status"
      >
        <span className="status-dot" />
        {hours}
        <span className="status-connected">Connected</span>
      </div>
    );
  }

  return (
    <div className="status-banner closed" role="status">
      <span className="status-dot" />
      Closed
      {status.openingTime ? `: orders open again at ${status.openingTime}` : ""}
      <span className="status-connected">Connected</span>
    </div>
  );
}

function Menu({
  user,
  token,
  onLogout,
  onNavigate,
  view = "menu",
  cart,
  setCart,
  cartAlert = false,
  onCartSeen,
  newOrderCount = 0,
  onOrderPlaced,
}) {
  const [menuItems, setMenuItems] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState("All");
  const showCart = view === "cart";
  const [loading, setLoading] = useState(true);
  const [menuError, setMenuError] = useState("");
  const [orderMessage, setOrderMessage] = useState("");
  const [placingOrder, setPlacingOrder] = useState(false);
  const [lastOrder, setLastOrder] = useState(null);

  const [searchTerm, setSearchTerm] = useState("");
  const [sortOption, setSortOption] = useState("default");
  const [justAddedId, setJustAddedId] = useState(null);

  // NEW: item customisations (Sauce, Extras, note for the kitchen)
  const [customising, setCustomising] = useState(null); // { item, groups }
  const [loadingOptionsId, setLoadingOptionsId] = useState(null);
  const optionsCache = useRef(new Map()); // menuItemId -> groups (asked once per visit)
  const [cartNotice, setCartNotice] = useState("");

  // NEW: bumped to load the menu again (fresh stock numbers)
  const [menuReload, setMenuReload] = useState(0);

  // NEW: open/closed + connected
  const cafeteria = useCafeteriaStatus();
  const isClosed = cafeteria.loaded && cafeteria.connected && cafeteria.isOpen === false;

  useEffect(() => {
    const loadMenu = async () => {
      try {
        setLoading(true);
        setMenuError("");

        const response = await apiFetch("/api/menu-items");

        if (response.status === 401) {
          if (onLogout) onLogout();
          return;
        }

        if (!response.ok) {
          throw new Error(`Could not load menu. Server returned ${response.status}.`);
        }

        const data = await response.json();

        setMenuItems(
          data
            .filter((item) => item.available !== false)
            .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0))
        );
      } catch (error) {
        console.error("Menu loading error:", error);
        setMenuError("The menu could not be loaded from the server.");
      } finally {
        setLoading(false);
      }
    };

    loadMenu();
    // Reload when the login token changes, or after an order (fresh stock).
    // onLogout doesn't need to trigger it.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, menuReload]);

  const categories = useMemo(
    () => [
      "All",
      ...Array.from(new Set(menuItems.map((item) => getCategoryName(item)))),
    ],
    [menuItems]
  );

  // CHANGED: smart search ("burger" also finds "burgers")
  const filteredItems = useMemo(
    () =>
      menuItems
        .filter(
          (item) =>
            selectedCategory === "All" ||
            getCategoryName(item) === selectedCategory
        )
        .filter((item) =>
          matchesSearch(item, searchTerm, getCategoryName(item))
        )
        .sort((a, b) => {
          if (sortOption === "price-asc") return Number(a.price) - Number(b.price);
          if (sortOption === "price-desc") return Number(b.price) - Number(a.price);
          if (sortOption === "name-asc") return a.name.localeCompare(b.name);
          return 0;
        }),
    [menuItems, selectedCategory, searchTerm, sortOption]
  );

  const goHome = () => {
    setLastOrder(null);
    onNavigate("menu");
    setSelectedCategory("All");
    setSearchTerm("");
    setSortOption("default");
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const showCartNotice = (message) => {
    setCartNotice(message);
    setTimeout(() => setCartNotice(""), 3000);
  };

  // Quantity already in the cart for one cart line
  const getLineQuantity = (lineId) =>
    cart.find((line) => lineIdOf(line) === lineId)?.quantity ?? 0;

  // How many of one menu item are in the cart (all option combinations)
  const getItemQuantity = (menuItemId) =>
    cart
      .filter((line) => line.menuItemId === menuItemId)
      .reduce((total, line) => total + line.quantity, 0);

  // NEW: stock left for one item after what's already in the cart
  // (Infinity = no stock number from the backend)
  const stockById = useMemo(
    () => new Map(menuItems.map((item) => [item.menuItemId, stockOf(item)])),
    [menuItems]
  );

  const stockLeftFor = (menuItemId) => {
    const stock = stockById.get(menuItemId);
    if (stock === null || stock === undefined) return Infinity;
    return stock - getItemQuantity(menuItemId);
  };

  const stockNotice = (item) => {
    const stock = stockById.get(item.menuItemId) ?? 0;
    const inCart = getItemQuantity(item.menuItemId);
    if (stock <= 0) return `Sorry, ${item.name} is sold out.`;
    return `Sorry, only ${stock} × ${item.name} left${
      inCart > 0 ? ` and ${inCart} ${inCart === 1 ? "is" : "are"} already in your cart` : ""
    }.`;
  };

  const flashAdded = (menuItemId) => {
    setJustAddedId(menuItemId);
    setTimeout(() => {
      setJustAddedId((currentId) => (currentId === menuItemId ? null : currentId));
    }, 1200);
  };

  // Puts an item (with its chosen options) in the cart.
  // Same item + same options + same note = one line; otherwise a new line.
  const addLine = (item, modifiers = [], note = "", quantity = 1) => {
    const lineId = makeLineId(item.menuItemId, modifiers, note);
    const alreadyInCart = getLineQuantity(lineId);

    if (alreadyInCart + quantity > MAX_QTY_PER_ITEM) {
      showCartNotice(`You can order at most ${MAX_QTY_PER_ITEM} of ${item.name}.`);
      return false;
    }

    // NEW: not more than the kitchen has left
    if (quantity > stockLeftFor(item.menuItemId)) {
      showCartNotice(stockNotice(item));
      return false;
    }

    setOrderMessage("");
    setLastOrder(null);
    flashAdded(item.menuItemId);

    setCart((currentCart) => {
      if (currentCart.some((line) => lineIdOf(line) === lineId)) {
        return currentCart.map((line) =>
          lineIdOf(line) === lineId
            ? { ...line, quantity: line.quantity + quantity }
            : line
        );
      }

      return [
        ...currentCart,
        {
          ...item,
          lineId,
          quantity,
          modifiers: modifiers.map(({ id, name, price }) => ({ id, name, price })),
          specialInstructions: note,
        },
      ];
    });

    return true;
  };

  // Asks the backend once per item which options it has (cached afterwards)
  const loadOptions = async (item) => {
    if (optionsCache.current.has(item.menuItemId)) {
      return optionsCache.current.get(item.menuItemId);
    }

    try {
      const response = await apiFetch(`/api/menu-items/${item.menuItemId}/customizations`);

      if (response.status === 401) {
        if (onLogout) onLogout();
        return null;
      }

      // No options or an error: the item can still be ordered plainly
      if (!response.ok) {
        console.warn(`Customisations returned ${response.status} for item ${item.menuItemId}`);
        return [];
      }

      const groups = normalizeGroups(await response.json(), item.menuItemId);
      optionsCache.current.set(item.menuItemId, groups);
      return groups;
    } catch (error) {
      console.warn("Could not load customisations:", error);
      return [];
    }
  };

  // "Add to Cart": items with options open the Customise sheet,
  // items without options go straight into the cart (one tap)
  const addToCart = async (item) => {
    // NEW: sold out (or the rest is already in the cart): don't open options
    if (stockLeftFor(item.menuItemId) < 1) {
      showCartNotice(stockNotice(item));
      return;
    }

    setLoadingOptionsId(item.menuItemId);
    const groups = await loadOptions(item);
    setLoadingOptionsId(null);

    if (groups === null) return; // logged out

    if (groups.length > 0) {
      setCustomising({ item, groups });
      return;
    }

    addLine(item);
  };

  // Called by the Customise sheet's "Add to cart" button
  const addCustomised = ({ modifiers, note, quantity }) => {
    const { item } = customising;

    if (addLine(item, modifiers, note, quantity)) {
      setCustomising(null);
      showCartNotice(`${quantity} × ${item.name} added to your cart.`);
    }
  };

  const increaseQuantity = (lineId) => {
    if (getLineQuantity(lineId) >= MAX_QTY_PER_ITEM) {
      showCartNotice(`You can order at most ${MAX_QTY_PER_ITEM} of one item.`);
      return;
    }

    // NEW: not more than the kitchen has left
    const line = cart.find((cartLine) => lineIdOf(cartLine) === lineId);
    if (line && stockLeftFor(line.menuItemId) < 1) {
      showCartNotice(stockNotice(line));
      return;
    }

    setCart((currentCart) =>
      currentCart.map((line) =>
        lineIdOf(line) === lineId ? { ...line, quantity: line.quantity + 1 } : line
      )
    );
  };

  const decreaseQuantity = (lineId) => {
    setCart((currentCart) =>
      currentCart
        .map((line) =>
          lineIdOf(line) === lineId ? { ...line, quantity: line.quantity - 1 } : line
        )
        .filter((line) => line.quantity > 0)
    );
  };

  const removeFromCart = (lineId) => {
    setCart((currentCart) => currentCart.filter((line) => lineIdOf(line) !== lineId));
  };

  const clearCart = () => {
    setCart([]);
    setOrderMessage("");
  };

  const cartCount = cart.reduce((total, item) => total + item.quantity, 0);

  // Includes the price of any extras chosen
  const cartTotal = cart.reduce((total, line) => total + unitPrice(line) * line.quantity, 0);

  const placeOrder = async () => {
    if (cart.length === 0) {
      setOrderMessage("Your cart is empty.");
      return;
    }

    if (!user?.userId) {
      setOrderMessage("Your user account could not be identified. Please log in again.");
      return;
    }

    try {
      setPlacingOrder(true);
      setOrderMessage("");

      const orderRequest = {
        userId: user.userId,
        items: cart.map((line) => ({
          menuItemId: line.menuItemId,
          quantity: line.quantity,
          specialInstructions: line.specialInstructions || "",
          modifierIds: (line.modifiers || []).map((modifier) => modifier.id),
        })),
      };

      const response = await apiFetch("/api/orders/checkout", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(orderRequest),
      });

      if (response.status === 401) {
        if (onLogout) onLogout();
        return;
      }

      if (!response.ok) {
        let errorMessage = `Order failed. Server returned ${response.status}.`;

        try {
          const errorData = await response.json();
          errorMessage = errorFromBody(errorData) || errorMessage;
        } catch {
          // Response did not contain JSON.
        }

        // NEW: something sold out while the student was choosing
        if (isStockError(response.status, errorMessage)) {
          setMenuReload((count) => count + 1);
          errorMessage =
            "Sorry, something in your cart has just sold out or there are fewer left than you chose. " +
            "The menu has been updated, please check your cart and try again.";
        }

        throw new Error(errorMessage);
      }

      const order = await response.json();

      setLastOrder(order);
      setCart([]);
      setMenuReload((count) => count + 1); // NEW: fresh stock numbers
      if (onOrderPlaced) onOrderPlaced(order);
    } catch (error) {
      console.error("Checkout error:", error);
      setOrderMessage(error.message || "Could not place your order.");
    } finally {
      setPlacingOrder(false);
    }
  };

  const openCart = () => {
    onNavigate("cart");
  };

  const backToMenu = () => {
    setLastOrder(null);
    onNavigate("menu");
  };

  // ---------- shared pieces ----------

  // Shared top bar + ☰ side menu + Help (same on every page)
  const header = (
    <AppHeader
      user={user}
      current={showCart ? "cart" : "menu"}
      cartCount={cartCount}
      cartAlert={cartAlert}
      onCartSeen={onCartSeen}
      newOrderCount={newOrderCount}
      onNavigate={(page) => (page === "menu" ? goHome() : onNavigate(page))}
      onLogout={onLogout}
    />
  );

  const notice = cartNotice && (
    <p className="cart-notice" role="status">
      {cartNotice}
    </p>
  );

  // ---------- order confirmation ----------

  const renderOrderConfirmation = () => (
    <div className="menu-card confirmation-card">
      <h3>✅ Order placed!</h3>

      <p>
        Order number: <strong>{lastOrder.customerOrderNumber}</strong>
      </p>

      <p>
        Status: <strong>{lastOrder.status}</strong>
      </p>

      {Array.isArray(lastOrder.items) && lastOrder.items.length > 0 && (
        <div className="confirmation-items">
          {lastOrder.items.map((item, index) => (
            <div key={index} className="confirmation-item">
              <p>
                {item.quantity} × {item.menuItemName} —{" "}
                {formatRand(orderLineTotal(item))}
              </p>
              {item.selectedModifiers?.length > 0 && (
                <small>{item.selectedModifiers.join(", ")}</small>
              )}
              {item.specialInstructions && (
                <small className="cart-item-note">Note: “{item.specialInstructions}”</small>
              )}
            </div>
          ))}
        </div>
      )}

      <h2 className="confirmation-total">
        Amount to pay at the till: {formatRand(lastOrder.totalPrice)}
      </h2>

      <p>
        Please pay at the cafeteria pay station when you collect your order.
        Show your order number to the cashier.
      </p>

      <NotifyPrompt />

      <div className="confirmation-actions">
        <button className="cart-button" onClick={backToMenu}>
          Back to Menu
        </button>

        <button
          className="cart-button"
          onClick={() => {
            setLastOrder(null);
            onNavigate("orders");
          }}
        >
          View My Orders
        </button>
      </div>
    </div>
  );

  // ---------- cart page ----------

  if (showCart) {
    return (
      <div className="menu-page">
        {header}

        {/* key: a new key = a new page, so the slide-in animation plays again */}
        <main key={lastOrder ? "confirmation" : "cart"} className="menu-content page-enter">
          {notice}
          <StatusBanner status={cafeteria} />

          {lastOrder ? (
            <>
              <section className="welcome-section">
                <h2>Thank you, {user?.firstName || "student"}!</h2>
                <p>Your order has been sent to the cafeteria.</p>
              </section>

              {renderOrderConfirmation()}
            </>
          ) : (
            <>
              <section className="welcome-section">
                <h2>Your Cart</h2>
                <p>Review your order before placing it.</p>
              </section>

              {cart.length === 0 ? (
                <div className="menu-card empty-cart">
                  <h3>Your cart is empty</h3>
                  <p>Add something from the menu to get started.</p>
                  <button className="cart-button" onClick={backToMenu}>
                    Browse Menu
                  </button>
                </div>
              ) : (
                <section className="cart-list">
                  {cart.map((item) => (
                    <article className="cart-item" key={lineIdOf(item)}>
                      <ItemImage item={item} categoryName={getCategoryName(item)} />

                      <div className="cart-item-info">
                        <h3>{item.name}</h3>

                        {/* NEW: chosen options and note */}
                        {item.modifiers?.length > 0 && (
                          <p className="cart-item-options">
                            {describeModifiers(item.modifiers, formatRand)}
                          </p>
                        )}
                        {item.specialInstructions && (
                          <p className="cart-item-note">Note: “{item.specialInstructions}”</p>
                        )}

                        <p>{formatRand(unitPrice(item))} each</p>

                        {/* NEW: more in the cart than the kitchen has left */}
                        {stockLeftFor(item.menuItemId) < 0 && (
                          <p className="cart-item-stock" role="alert">
                            {stockNotice(item)} Please remove{" "}
                            {-stockLeftFor(item.menuItemId)}.
                          </p>
                        )}

                        <div className="qty-controls">
                          <button
                            onClick={() => decreaseQuantity(lineIdOf(item))}
                            aria-label={`One less ${item.name}`}
                          >
                            −
                          </button>

                          <span>{item.quantity}</span>

                          <button
                            onClick={() => increaseQuantity(lineIdOf(item))}
                            disabled={
                              item.quantity >= MAX_QTY_PER_ITEM || stockLeftFor(item.menuItemId) < 1
                            }
                            aria-label={`One more ${item.name}`}
                          >
                            +
                          </button>

                          <button
                            className="remove-button"
                            onClick={() => removeFromCart(lineIdOf(item))}
                          >
                            Remove
                          </button>
                        </div>
                      </div>

                      <strong className="cart-item-total">
                        {formatRand(unitPrice(item) * item.quantity)}
                      </strong>
                    </article>
                  ))}
                </section>
              )}

              {cart.length > 0 && (
                <div className="checkout-box">
                  <h2>Total: {formatRand(cartTotal)}</h2>
                  <p>Payment is made at the cafeteria pay station when you collect.</p>

                  {isClosed && (
                    <p className="checkout-closed">
                      The cafeteria is closed right now
                      {cafeteria.openingTime
                        ? `. Orders open again at ${cafeteria.openingTime}.`
                        : "."}
                    </p>
                  )}

                  {orderMessage && (
                    <p className="checkout-message">
                      <strong>{orderMessage}</strong>
                    </p>
                  )}

                  <div className="checkout-actions">
                    <button className="cart-button secondary" onClick={clearCart}>
                      Clear Cart
                    </button>

                    <button
                      className="cart-button"
                      onClick={placeOrder}
                      disabled={placingOrder || isClosed}
                    >
                      {placingOrder ? "Placing Order..." : "Place Order"}
                    </button>
                  </div>
                </div>
              )}
            </>
          )}
        </main>
      </div>
    );
  }

  // ---------- menu page ----------

  return (
    <div className="menu-page">
      {header}

      <main key="menu" className="menu-content page-enter">
        {notice}
        <StatusBanner status={cafeteria} />

        <section className="welcome-section">
          <h2>
            {user?.firstName ? `Welcome, ${user.firstName}` : "What's on the menu?"}
          </h2>
          <p>Browse today's meals and order ahead.</p>
        </section>

        {menuError && <p className="menu-error">{menuError}</p>}

        {!menuError && (
          <>
            {/* CHANGED: search + sort + categories stick to the top while scrolling */}
            <div className="menu-sticky">
              <div className="menu-toolbar">
                <label className="menu-search">
                  <span aria-hidden="true">⌕</span>
                  <input
                    type="search"
                    placeholder="Search, e.g. burger or coffee"
                    value={searchTerm}
                    maxLength={50}
                    onChange={(event) => setSearchTerm(event.target.value)}
                  />
                </label>

                {/* CHANGED: compact sort button next to the search box.
                    The real dropdown sits invisibly on top, so tapping it
                    opens the phone's normal picker. */}
                <label
                  className={`menu-sort ${sortOption !== "default" ? "active" : ""}`}
                  title="Sort the menu"
                >
                  <span className="menu-sort-icon" aria-hidden="true">⇅</span>
                  <span className="menu-sort-label">
                    {SORT_OPTIONS.find((option) => option.value === sortOption)?.label}
                  </span>
                  <select
                    value={sortOption}
                    onChange={(event) => setSortOption(event.target.value)}
                    aria-label="Sort menu"
                  >
                    {SORT_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              {/* CHANGED: sideways row with fades + arrows so it's clear it scrolls */}
              <CategoryBar
                categories={categories}
                selected={selectedCategory}
                onSelect={setSelectedCategory}
              />
            </div>

            <section className="menu-grid">
              {/* NEW: grey placeholder cards while loading */}
              {loading &&
                Array.from({ length: 6 }).map((_, index) => (
                  <div className="menu-card skeleton" key={`skeleton-${index}`}>
                    <div className="menu-card-image" />
                    <div className="skeleton-line wide" />
                    <div className="skeleton-line" />
                  </div>
                ))}

              {!loading &&
                filteredItems.map((item, index) => {
                  const categoryName = getCategoryName(item);
                  const added = justAddedId === item.menuItemId;
                  const loadingOptions = loadingOptionsId === item.menuItemId;

                  // The plain version (no options, no note) gets a − 2 + stepper
                  // right on the card, so ordering several doesn't need the cart page
                  const plainLineId = makeLineId(item.menuItemId);
                  const plainQuantity = getLineQuantity(plainLineId);
                  // Versions with options are counted too, e.g. "2 in cart"
                  const inCart = getItemQuantity(item.menuItemId);

                  // NEW: stock
                  const stock = stockOf(item);
                  const soldOut = stock === 0;
                  const noneLeftToAdd = stockLeftFor(item.menuItemId) < 1;

                  return (
                    <article
                      className={`menu-card ${soldOut ? "sold-out" : ""}`}
                      key={item.menuItemId}
                      style={{ animationDelay: `${Math.min(index, 10) * 30}ms` }}
                    >
                      <ItemImage item={item} categoryName={categoryName} />

                      <span className="category">{categoryName}</span>

                      <h3>{item.name}</h3>

                      <p>{item.description}</p>

                      {/* NEW: stock label */}
                      {soldOut ? (
                        <span className="stock-label stock-out">Sold out</span>
                      ) : (
                        stock !== null &&
                        stock <= LOW_STOCK && (
                          <span className="stock-label stock-low">Only {stock} left</span>
                        )
                      )}

                      <div className="menu-card-footer">
                        <div className="menu-card-price">
                          <strong>{formatRand(item.price)}</strong>
                          {inCart > plainQuantity && (
                            <small className="menu-card-in-cart">{inCart} in cart</small>
                          )}
                        </div>

                        {plainQuantity > 0 ? (
                          <div className="card-stepper" role="group" aria-label={`${item.name} quantity`}>
                            <button
                              type="button"
                              onClick={() => decreaseQuantity(plainLineId)}
                              aria-label={
                                plainQuantity === 1
                                  ? `Remove ${item.name} from cart`
                                  : `One less ${item.name}`
                              }
                            >
                              −
                            </button>

                            <span key={plainQuantity} aria-live="polite">
                              {plainQuantity}
                            </span>

                            <button
                              type="button"
                              onClick={() => addToCart(item)}
                              disabled={
                                loadingOptions || plainQuantity >= MAX_QTY_PER_ITEM || noneLeftToAdd
                              }
                              aria-label={`One more ${item.name}`}
                            >
                              +
                            </button>
                          </div>
                        ) : (
                          <button
                            className={`add-button ${added ? "added" : ""}`}
                            onClick={() => addToCart(item)}
                            disabled={loadingOptions || soldOut}
                          >
                            {soldOut
                              ? "Sold out"
                              : loadingOptions
                                ? "Loading…"
                                : added
                                  ? "Added ✓"
                                  : "Add to Cart"}
                          </button>
                        )}
                      </div>
                    </article>
                  );
                })}
            </section>

            {!loading && filteredItems.length === 0 && (
              <p className="menu-empty">
                {searchTerm.trim()
                  ? `No meals match "${searchTerm.trim()}".`
                  : "No menu items are currently available."}
              </p>
            )}
          </>
        )}
      </main>

      {/* NEW: floating cart bar */}
      {cartCount > 0 && (
        <button className="floating-cart" onClick={openCart}>
          <span key={cartCount} className="floating-cart-count">
            {cartCount}
          </span>
          <span>View cart</span>
          <strong>{formatRand(cartTotal)}</strong>
        </button>
      )}

      {/* NEW: options pop-up for items that can be customised */}
      {customising && (
        <CustomiseSheet
          item={customising.item}
          groups={customising.groups}
          maxQuantity={Math.max(1, Math.min(MAX_QTY_PER_ITEM, stockLeftFor(customising.item.menuItemId)))}
          formatRand={formatRand}
          onAdd={addCustomised}
          onClose={() => setCustomising(null)}
        />
      )}
    </div>
  );
}

export default Menu;
