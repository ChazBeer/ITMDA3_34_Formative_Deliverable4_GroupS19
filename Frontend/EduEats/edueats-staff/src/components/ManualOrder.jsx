// src/components/ManualOrder.jsx
// "+ New Manual Order": staff build an order for a walk-in customer at the
// counter. Pick items (with their options), add the customer's name, place it.
// The order is placed under the signed-in staff account; the customer's name
// is saved at the start of the first item's note ("Walk-in: Thabo").

import { useEffect, useMemo, useRef, useState } from "react";
import "./ManualOrder.css";
import { apiFetch } from "../api";
import { formatPrice, walkInNote } from "../orders";
import { describeRule, normalizeGroups, unitPrice } from "../customizations";
import ItemThumb from "./ItemThumb";
import { categoryOf } from "../menuItems";
import { readApiError } from "../apiError";

const MAX_QUANTITY = 10;
const NAME_MAX = 40;
const NOTE_MAX = 150;

// CHANGED: uses the shared reader (shows validationErrors too)
function readError(response, fallback) {
  return readApiError(response, fallback);
}

const lineKey = (itemId, modifiers, note) =>
  `${itemId}|${modifiers.map((m) => m.id).sort((a, b) => a - b).join(".")}|${note.trim().toLowerCase()}`;

function ManualOrder({ staffUser, onClose, onPlaced, onSessionExpired }) {
  const [menu, setMenu] = useState([]);
  const [loadingMenu, setLoadingMenu] = useState(true);
  const [menuError, setMenuError] = useState("");
  const [category, setCategory] = useState("All");
  const [search, setSearch] = useState("");

  const optionsCache = useRef(new Map()); // menuItemId -> groups
  const [loadingId, setLoadingId] = useState(null);
  const [picking, setPicking] = useState(null); // { item, groups, selected, quantity, note }

  const [lines, setLines] = useState([]);
  const [customerName, setCustomerName] = useState("");
  const [placing, setPlacing] = useState(false);
  const [error, setError] = useState("");

  // Keep the latest close handler for the Escape key
  const closeRef = useRef(onClose);
  useEffect(() => {
    closeRef.current = onClose;
  }, [onClose]);

  // Escape closes; the page behind doesn't scroll
  useEffect(() => {
    const onKey = (event) => {
      if (event.key === "Escape") closeRef.current();
    };
    window.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, []);

  // Load the menu once
  useEffect(() => {
    let cancelled = false;

    (async () => {
      try {
        const response = await apiFetch("/api/menu-items");
        if (response.status === 401) {
          onSessionExpired();
          return;
        }
        if (!response.ok) throw new Error(`Server returned ${response.status}`);
        const data = await response.json();
        if (cancelled) return;
        setMenu(
          data
            .filter((item) => item.available !== false)
            .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0))
        );
      } catch (err) {
        console.error("Manual order menu error:", err);
        if (!cancelled) setMenuError("The menu could not be loaded. Try again in a moment.");
      } finally {
        if (!cancelled) setLoadingMenu(false);
      }
    })();

    return () => {
      cancelled = true;
    };
    // Load once when the pop-up opens
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);


  const categories = useMemo(
    () => ["All", ...Array.from(new Set(menu.map(categoryOf)))],
    [menu]
  );

  const shownItems = useMemo(() => {
    const term = search.trim().toLowerCase();
    return menu.filter(
      (item) =>
        (category === "All" || categoryOf(item) === category) &&
        (!term || `${item.name} ${categoryOf(item)}`.toLowerCase().includes(term))
    );
  }, [menu, category, search]);

  const total = lines.reduce((sum, line) => sum + unitPrice(line) * line.quantity, 0);
  const itemCount = lines.reduce((sum, line) => sum + line.quantity, 0);

  // NEW: stock left after what's already on the ticket (Infinity = not tracked)
  const stockLeft = (item) => {
    if (item.stockQuantity === undefined || item.stockQuantity === null) return Infinity;
    const onTicket = lines
      .filter((line) => line.item.menuItemId === item.menuItemId)
      .reduce((sum, line) => sum + line.quantity, 0);
    return Number(item.stockQuantity) - onTicket;
  };

  const stockError = (item) =>
    Number(item.stockQuantity) <= 0
      ? `${item.name} is sold out.`
      : `Only ${item.stockQuantity} × ${item.name} left.`;

  // Adds to the ticket; same item + options + note = one line
  const addLine = (item, modifiers = [], note = "", quantity = 1) => {
    if (quantity > stockLeft(item)) {
      setError(stockError(item));
      return;
    }
    const key = lineKey(item.menuItemId, modifiers, note);
    setError("");
    setLines((current) => {
      const existing = current.find((line) => line.key === key);
      if (existing) {
        return current.map((line) =>
          line.key === key
            ? { ...line, quantity: Math.min(MAX_QUANTITY, line.quantity + quantity) }
            : line
        );
      }
      return [...current, { key, item, modifiers, note: note.trim(), quantity }];
    });
  };

  const loadOptions = async (item) => {
    if (optionsCache.current.has(item.menuItemId)) {
      return optionsCache.current.get(item.menuItemId);
    }
    try {
      const response = await apiFetch(`/api/menu-items/${item.menuItemId}/customizations`);
      if (response.status === 401) {
        onSessionExpired();
        return null;
      }
      const groups = response.ok ? normalizeGroups(await response.json()) : [];
      optionsCache.current.set(item.menuItemId, groups);
      return groups;
    } catch {
      return [];
    }
  };

  // Tap an item: no options = straight onto the ticket, otherwise pick options
  const chooseItem = async (item) => {
    if (stockLeft(item) < 1) {
      setError(stockError(item));
      return;
    }
    setLoadingId(item.menuItemId);
    const groups = await loadOptions(item);
    setLoadingId(null);
    if (groups === null) return;

    if (groups.length === 0) {
      addLine(item);
      return;
    }

    setPicking({ item, groups, selected: {}, quantity: 1, note: "" });
  };

  const toggleOption = (group, optionId) => {
    setPicking((current) => {
      const chosen = current.selected[group.id] || [];
      let next;
      if (group.max === 1) next = chosen.includes(optionId) ? [] : [optionId];
      else if (chosen.includes(optionId)) next = chosen.filter((id) => id !== optionId);
      else if (chosen.length < group.max) next = [...chosen, optionId];
      else next = chosen; // already at the maximum
      return { ...current, selected: { ...current.selected, [group.id]: next } };
    });
  };

  const missingGroup = picking?.groups.find(
    (group) => (picking.selected[group.id] || []).length < group.min
  );

  const pickedModifiers = picking
    ? picking.groups.flatMap((group) =>
        group.options.filter((option) => (picking.selected[group.id] || []).includes(option.id))
      )
    : [];

  const addPicked = () => {
    if (!picking || missingGroup) return;
    addLine(picking.item, pickedModifiers, picking.note, picking.quantity);
    setPicking(null);
  };

  const changeQuantity = (key, change) => {
    const changing = lines.find((line) => line.key === key);
    if (changing && change > 0 && stockLeft(changing.item) < change) {
      setError(stockError(changing.item));
      return;
    }
    setLines((current) =>
      current
        .map((line) =>
          line.key === key
            ? { ...line, quantity: Math.min(MAX_QUANTITY, line.quantity + change) }
            : line
        )
        .filter((line) => line.quantity > 0)
    );
  };

  const placeOrder = async () => {
    if (lines.length === 0 || placing) return;
    setPlacing(true);
    setError("");

    try {
      const response = await apiFetch("/api/orders/checkout", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          userId: staffUser?.userId,
          items: lines.map((line, index) => ({
            menuItemId: line.item.menuItemId,
            quantity: line.quantity,
            specialInstructions:
              index === 0 ? walkInNote(customerName, line.note) : line.note,
            modifierIds: line.modifiers.map((modifier) => modifier.id),
          })),
        }),
      });

      if (response.status === 401) {
        onSessionExpired();
        return;
      }

      if (!response.ok) {
        throw new Error(
          await readError(response, `The order could not be placed (status ${response.status}).`)
        );
      }

      onPlaced(await response.json());
    } catch (err) {
      console.error("Manual order error:", err);
      setError(
        err.message === "Failed to fetch"
          ? "Cannot reach the server. Is the backend running?"
          : err.message
      );
    } finally {
      setPlacing(false);
    }
  };

  return (
    <div className="manual-overlay" onClick={onClose}>
      <div
        className="manual-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="manual-title"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="manual-header">
          <div>
            <h2 id="manual-title">New manual order</h2>
            <p>For walk-in customers ordering at the counter.</p>
          </div>
          <button type="button" className="manual-close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </header>

        <div className="manual-body">
          {/* ---------- left: menu or options ---------- */}
          <section className="manual-menu" aria-label="Menu">
            {picking ? (
              <div className="manual-options">
                <button type="button" className="manual-back" onClick={() => setPicking(null)}>
                  ← Back to menu
                </button>

                <h3>
                  {picking.item.name} <span>{formatPrice(picking.item.price)}</span>
                </h3>

                {picking.groups.map((group) => {
                  const chosen = picking.selected[group.id] || [];
                  return (
                    <fieldset key={group.id} className="manual-group">
                      <legend>
                        {group.name}
                        <small className={group.min > 0 ? "required" : ""}>
                          {describeRule(group)}
                        </small>
                      </legend>

                      {group.options.map((option) => (
                        <label key={option.id} className="manual-option">
                          <input
                            type={group.max === 1 ? "radio" : "checkbox"}
                            name={`group-${group.id}`}
                            checked={chosen.includes(option.id)}
                            onChange={() => toggleOption(group, option.id)}
                            onClick={() => {
                              // a radio can't be unticked by itself: tap again to clear
                              if (group.max === 1 && group.min === 0 && chosen.includes(option.id)) {
                                toggleOption(group, option.id);
                              }
                            }}
                          />
                          <span>{option.name}</span>
                          {option.price > 0 && <em>+{formatPrice(option.price)}</em>}
                        </label>
                      ))}
                    </fieldset>
                  );
                })}

                <label className="manual-field">
                  Note for the kitchen <small>Optional</small>
                  <input
                    type="text"
                    value={picking.note}
                    maxLength={NOTE_MAX}
                    placeholder="e.g. no onions"
                    onChange={(event) => setPicking({ ...picking, note: event.target.value })}
                  />
                </label>

                <div className="manual-picker-actions">
                  <div className="manual-stepper" role="group" aria-label="Quantity">
                    <button
                      type="button"
                      onClick={() => setPicking({ ...picking, quantity: Math.max(1, picking.quantity - 1) })}
                      disabled={picking.quantity <= 1}
                      aria-label="One less"
                    >
                      −
                    </button>
                    <span>{picking.quantity}</span>
                    <button
                      type="button"
                      onClick={() =>
                        setPicking({
                          ...picking,
                          quantity: Math.min(MAX_QUANTITY, stockLeft(picking.item), picking.quantity + 1),
                        })
                      }
                      disabled={picking.quantity >= Math.min(MAX_QUANTITY, stockLeft(picking.item))}
                      aria-label="One more"
                    >
                      +
                    </button>
                  </div>

                  <button
                    type="button"
                    className="primary-button"
                    onClick={addPicked}
                    disabled={Boolean(missingGroup)}
                  >
                    {missingGroup
                      ? `Choose ${missingGroup.name}`
                      : `Add · ${formatPrice(
                          unitPrice({ item: picking.item, modifiers: pickedModifiers }) * picking.quantity
                        )}`}
                  </button>
                </div>
              </div>
            ) : (
              <>
                <div className="manual-search">
                  <span aria-hidden="true">⌕</span>
                  <input
                    type="search"
                    placeholder="Search the menu"
                    value={search}
                    maxLength={50}
                    onChange={(event) => setSearch(event.target.value)}
                  />
                </div>

                <div className="manual-categories">
                  {categories.map((name) => (
                    <button
                      key={name}
                      type="button"
                      className={category === name ? "active" : ""}
                      onClick={() => setCategory(name)}
                    >
                      {name}
                    </button>
                  ))}
                </div>

                {loadingMenu && <p className="manual-muted">Loading the menu…</p>}
                {menuError && <p className="manual-error">{menuError}</p>}
                {!loadingMenu && !menuError && shownItems.length === 0 && (
                  <p className="manual-muted">No items match.</p>
                )}

                <div className="manual-items">
                  {shownItems.map((item) => (
                    <button
                      key={item.menuItemId}
                      type="button"
                      className="manual-item"
                      onClick={() => chooseItem(item)}
                      disabled={loadingId === item.menuItemId || Number(item.stockQuantity) === 0}
                    >
                      <ItemThumb item={item} size="medium" />
                      <span className="manual-item-text">
                        <span className="manual-item-name">{item.name}</span>
                        <span className="manual-item-meta">
                          {categoryOf(item)} · {formatPrice(item.price)}
                          {/* NEW: stock */}
                          {Number(item.stockQuantity) === 0 ? (
                            <strong className="manual-stock out"> · Sold out</strong>
                          ) : (
                            item.stockQuantity !== undefined &&
                            item.stockQuantity !== null &&
                            item.stockQuantity <= 5 && (
                              <strong className="manual-stock"> · {item.stockQuantity} left</strong>
                            )
                          )}
                        </span>
                      </span>
                      <span className="manual-item-add" aria-hidden="true">
                        {loadingId === item.menuItemId ? "…" : "+"}
                      </span>
                    </button>
                  ))}
                </div>
              </>
            )}
          </section>

          {/* ---------- right: the ticket ---------- */}
          <section className="manual-ticket" aria-label="Order">
            <label className="manual-field">
              Customer name <small>Optional</small>
              <input
                type="text"
                value={customerName}
                maxLength={NAME_MAX}
                placeholder="e.g. Thabo"
                onChange={(event) => setCustomerName(event.target.value)}
              />
            </label>

            {lines.length === 0 ? (
              <p className="manual-empty">Tap items on the menu to add them here.</p>
            ) : (
              <ul className="manual-lines">
                {lines.map((line) => (
                  <li key={line.key}>
                    <div className="manual-line-info">
                      <strong>{line.item.name}</strong>
                      {line.modifiers.length > 0 && (
                        <small>{line.modifiers.map((modifier) => modifier.name).join(", ")}</small>
                      )}
                      {line.note && <small className="manual-note">Note: “{line.note}”</small>}
                    </div>

                    <div className="manual-stepper small" role="group" aria-label={`${line.item.name} quantity`}>
                      <button
                        type="button"
                        onClick={() => changeQuantity(line.key, -1)}
                        aria-label={line.quantity === 1 ? `Remove ${line.item.name}` : `One less ${line.item.name}`}
                      >
                        −
                      </button>
                      <span>{line.quantity}</span>
                      <button
                        type="button"
                        onClick={() => changeQuantity(line.key, 1)}
                        disabled={line.quantity >= MAX_QUANTITY}
                        aria-label={`One more ${line.item.name}`}
                      >
                        +
                      </button>
                    </div>

                    <span className="manual-line-total">
                      {formatPrice(unitPrice(line) * line.quantity)}
                    </span>
                  </li>
                ))}
              </ul>
            )}

            <div className="manual-total">
              <span>
                Total{itemCount > 0 ? ` · ${itemCount} item${itemCount > 1 ? "s" : ""}` : ""}
              </span>
              <strong>{formatPrice(total)}</strong>
            </div>

            {error && (
              <p className="manual-error" role="alert">
                {error}
              </p>
            )}

            <button
              type="button"
              className="primary-button manual-place"
              onClick={placeOrder}
              disabled={lines.length === 0 || placing}
            >
              {placing ? "Placing order…" : "Place order"}
            </button>
            <p className="manual-muted small">Payment is taken at the till as usual.</p>
          </section>
        </div>
      </div>
    </div>
  );
}

export default ManualOrder;
