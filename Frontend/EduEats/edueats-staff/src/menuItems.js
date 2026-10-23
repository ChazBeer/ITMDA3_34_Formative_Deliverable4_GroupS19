// src/menuItems.js
// The full menu (including sold-out items) for the staff app:
// - pictures for the order cards and kitchen view
// - the "Sold out" panel
// GET /api/menu-items/admin/all (staff + admin)

import { useCallback, useEffect, useMemo, useState } from "react";
import { apiFetch } from "./api";

export const categoryOf = (item) =>
  item?.category?.categoryName || item?.categoryName || "Other";

// Full address of an item's picture, or null if it has none yet
export function imageUrlOf(item) {
  const path = item?.imagePath;
  if (!path) return null;
  if (/^https?:\/\//i.test(path)) return path;
  return path.startsWith("/") ? path : `/${path}`;
}

// Emoji shown instead of a picture, picked from the category name
const CATEGORY_ICONS = [
  ["breakfast", "🍳"],
  ["burger", "🍔"],
  ["toast", "🥪"],
  ["sandwich", "🥪"],
  ["hot drink", "☕"],
  ["drink", "🥤"],
  ["dessert", "🍰"],
  ["snack", "🍟"],
  ["side", "🍟"],
  ["meal", "🍛"],
  ["favourite", "🌭"],
];

export function iconFor(categoryName) {
  const name = String(categoryName || "").toLowerCase();
  return CATEGORY_ICONS.find(([word]) => name.includes(word))?.[1] || "🍽️";
}

const normaliseName = (name) => String(name || "").trim().toLowerCase();

// enabled: only loads while logged in
function useMenuItems(enabled) {
  const [items, setItems] = useState([]);
  const [error, setError] = useState("");

  const reload = useCallback(async () => {
    try {
      const response = await apiFetch("/api/menu-items/admin/all");
      if (!response.ok) throw new Error(`Server returned ${response.status}`);
      const data = await response.json();
      setItems(
        [...data].sort(
          (a, b) =>
            categoryOf(a).localeCompare(categoryOf(b)) ||
            (a.displayOrder ?? 0) - (b.displayOrder ?? 0)
        )
      );
      setError("");
    } catch (err) {
      console.warn("Could not load the menu for pictures/sold out:", err);
      setError("The menu could not be loaded.");
    }
  }, []);

  useEffect(() => {
    if (!enabled) return undefined;
    const first = setTimeout(reload, 0);
    return () => clearTimeout(first);
  }, [enabled, reload]);

  // Orders only send the item's name, so pictures are looked up by name
  const byName = useMemo(() => {
    const map = new Map();
    items.forEach((item) => map.set(normaliseName(item.name), item));
    return map;
  }, [items]);

  const findByName = useCallback((name) => byName.get(normaliseName(name)) || null, [byName]);

  return { items, setItems, error, reload, findByName };
}

export default useMenuItems;
