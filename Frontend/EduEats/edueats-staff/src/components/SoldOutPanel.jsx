// src/components/SoldOutPanel.jsx
// "Sold out" panel: staff mark items as sold out (hidden from the student
// menu straight away) and back in stock, in one tap.
//
// Uses PATCH /api/menu-items/{id}/availability?available=true|false
// (staff + admin). Older backends without it: admins fall back to the full update.
// Note: the backend also marks an item unavailable by itself when its stock reaches 0.

import { useEffect, useMemo, useRef, useState } from "react";
import "./SoldOutPanel.css";
import { apiFetch } from "../api";
import ItemThumb from "./ItemThumb";
import { categoryOf } from "../menuItems";
import { formatPrice } from "../orders";

async function saveAvailability(item, available, isAdmin) {
  // 1. The staff-friendly endpoint (only changes "available")
  let response = await apiFetch(
    `/api/menu-items/${item.menuItemId}/availability?available=${available}`,
    { method: "PATCH" }
  );

  // 2. Not on this backend yet: admins can use the full update instead
  if (!response.ok && [403, 404, 405].includes(response.status) && isAdmin) {
    response = await apiFetch(`/api/menu-items/${item.menuItemId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        name: item.name,
        description: item.description || "",
        price: Number(item.price),
        available,
        displayOrder: item.displayOrder ?? 0,
        // NEW: keep the stock as it is (the full update would reset it to 0)
        stockQuantity: item.stockQuantity ?? 0,
        category: { categoryId: item.category?.categoryId ?? item.categoryId },
      }),
    });
  }

  return response;
}

// NEW: stock left ("3 left", "0 left") or "" if the backend has no stock number
const LOW_STOCK = 5;
const hasStock = (item) => item.stockQuantity !== undefined && item.stockQuantity !== null;
const stockText = (item) => (hasStock(item) ? `${item.stockQuantity} left` : "");
const stockLevel = (item) =>
  !hasStock(item) ? "" : Number(item.stockQuantity) === 0 ? "out" : Number(item.stockQuantity) <= LOW_STOCK ? "low" : "";

function SoldOutPanel({ items, isAdmin, onChanged, onSessionExpired, onClose }) {
  const [search, setSearch] = useState("");
  const [onlySoldOut, setOnlySoldOut] = useState(false);
  const [savingId, setSavingId] = useState(null);
  const [error, setError] = useState("");
  const [note, setNote] = useState(""); // NEW: e.g. "available, but 0 left"

  const closeRef = useRef(onClose);
  useEffect(() => {
    closeRef.current = onClose;
  }, [onClose]);

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

  const soldOutCount = items.filter((item) => item.available === false).length;

  // Items grouped by category, filtered by the search box
  const groups = useMemo(() => {
    const term = search.trim().toLowerCase();
    const map = new Map();
    items
      .filter((item) => !onlySoldOut || item.available === false)
      .filter((item) => !term || `${item.name} ${categoryOf(item)}`.toLowerCase().includes(term))
      .forEach((item) => {
        const name = categoryOf(item);
        if (!map.has(name)) map.set(name, []);
        map.get(name).push(item);
      });
    return [...map.entries()];
  }, [items, search, onlySoldOut]);

  const toggle = async (item) => {
    const available = item.available === false; // sold out -> back in stock
    setSavingId(item.menuItemId);
    setError("");
    setNote("");

    try {
      const response = await saveAvailability(item, available, isAdmin);

      if (response.status === 401) {
        onSessionExpired();
        return;
      }

      if (response.status === 403) {
        throw new Error("Your account isn't allowed to change this. Please ask an admin.");
      }

      if (!response.ok) {
        throw new Error(`Could not update ${item.name} (status ${response.status}).`);
      }

      onChanged({ ...item, available });

      // Back "in stock" but nothing left: students still can't order it
      if (available && hasStock(item) && Number(item.stockQuantity) <= 0) {
        setNote(
          `${item.name} is switched on, but has 0 left. Set its stock in Admin Panel › Products so students can order it.`
        );
      }
    } catch (err) {
      console.error("Sold out toggle error:", err);
      setError(err.message === "Failed to fetch" ? "Cannot reach the server." : err.message);
    } finally {
      setSavingId(null);
    }
  };

  return (
    <div className="soldout-overlay" onClick={onClose}>
      <aside
        className="soldout-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="soldout-title"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="soldout-header">
          <div>
            <h2 id="soldout-title">What's available</h2>
            <p>
              {soldOutCount === 0
                ? "Everything is in stock."
                : `${soldOutCount} item${soldOutCount > 1 ? "s" : ""} sold out.`}{" "}
              Sold-out items are hidden from the student menu.
              {isAdmin ? " Change stock numbers in Admin Panel › Products." : ""}
            </p>
          </div>
          <button type="button" className="soldout-close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </header>

        <div className="soldout-tools">
          <input
            type="search"
            placeholder="Search items"
            value={search}
            maxLength={50}
            onChange={(event) => setSearch(event.target.value)}
          />
          <label>
            <input
              type="checkbox"
              checked={onlySoldOut}
              onChange={(event) => setOnlySoldOut(event.target.checked)}
            />
            Sold out only
          </label>
        </div>

        {note && (
          <p className="soldout-note" role="status">
            {note}
          </p>
        )}

        {error && (
          <p className="soldout-error" role="alert">
            {error}
          </p>
        )}

        <div className="soldout-list">
          {groups.map(([category, groupItems]) => (
            <section key={category}>
              <h3>{category}</h3>
              {groupItems.map((item) => {
                const soldOut = item.available === false;
                return (
                  <div className={`soldout-row ${soldOut ? "is-sold-out" : ""}`} key={item.menuItemId}>
                    <ItemThumb item={item} />
                    <div className="soldout-name">
                      <strong>{item.name}</strong>
                      <small>
                        {formatPrice(item.price)}
                        {/* NEW: stock left */}
                        {stockText(item) && (
                          <span className={`soldout-stock ${stockLevel(item)}`}> · {stockText(item)}</span>
                        )}
                      </small>
                    </div>
                    <button
                      type="button"
                      className={`soldout-switch ${soldOut ? "off" : "on"}`}
                      role="switch"
                      aria-checked={!soldOut}
                      aria-label={`${item.name}: ${soldOut ? "sold out" : "available"}`}
                      disabled={savingId === item.menuItemId}
                      onClick={() => toggle(item)}
                    >
                      <span className="soldout-knob" />
                      {soldOut ? "Sold out" : "Available"}
                    </button>
                  </div>
                );
              })}
            </section>
          ))}

          {groups.length === 0 && <p className="soldout-empty">No items match.</p>}
        </div>
      </aside>
    </div>
  );
}

export default SoldOutPanel;
