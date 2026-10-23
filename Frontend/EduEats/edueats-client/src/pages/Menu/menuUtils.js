// src/pages/Menu/menuUtils.js
// Small helpers for the menu page: smart search, images and categories.

// Turns "Burgers!" into "burger" so "burger" and "burgers" match.
function stem(word) {
  let w = word.toLowerCase().replace(/[^a-z0-9]/g, "");
  if (w.length > 4 && w.endsWith("ies")) return w.slice(0, -3) + "y";
  if (w.length > 4 && (w.endsWith("ches") || w.endsWith("shes") || w.endsWith("oes"))) {
    return w.slice(0, -2);
  }
  if (w.length > 3 && w.endsWith("s") && !w.endsWith("ss")) return w.slice(0, -1);
  return w;
}

// Each word in two forms: "toasties" -> ["toasty", "toastie"]
function words(text) {
  return String(text || "")
    .split(/\s+/)
    .flatMap((word) => {
      const plain = word.toLowerCase().replace(/[^a-z0-9]/g, "");
      const noS = plain.length > 3 && plain.endsWith("s") ? plain.slice(0, -1) : plain;
      return [stem(word), noS];
    })
    .filter(Boolean);
}

// True when every word typed matches the start of a word in the item
// e.g. "chick burg" finds "Chicken Burger", "burgers" finds "Burger"
export function matchesSearch(item, query, categoryName) {
  const queryWords = words(query);
  if (queryWords.length === 0) return true;

  const itemWords = words(
    `${item.name} ${item.description || ""} ${categoryName || ""}`
  );

  // Typed words come in pairs (two forms of the same word); one form must match
  const pairs = [];
  for (let i = 0; i < queryWords.length; i += 2) {
    pairs.push(queryWords.slice(i, i + 2));
  }

  return pairs.every((forms) =>
    forms.some((q) => itemWords.some((w) => w.startsWith(q)))
  );
}

export function getCategoryName(item) {
  return (
    item.categoryName ||
    item.category?.categoryName ||
    item.category?.name ||
    "Other"
  );
}

// Image from the backend: a full URL (e.g. Cloudinary) or a path on the server
export function getImageUrl(item) {
  const path = item.imagePath;
  if (!path) return null;
  if (/^https?:\/\//i.test(path)) return path;
  return path.startsWith("/") ? path : `/${path}`;
}

// Emoji shown on the placeholder when an item has no picture yet
const CATEGORY_ICONS = [
  ["breakfast", "🍳"],
  ["burger", "🍔"],
  ["wrap", "🌯"],
  ["toast", "🥪"],
  ["sandwich", "🥪"],
  ["hot meal", "🍛"],
  ["favourite", "🌭"],
  ["snack", "🍟"],
  ["side", "🍟"],
  ["dessert", "🍰"],
  ["hot drink", "☕"],
  ["drink", "🥤"],
];

export function getPlaceholderIcon(categoryName) {
  const name = String(categoryName || "").toLowerCase();
  const match = CATEGORY_ICONS.find(([key]) => name.includes(key));
  return match ? match[1] : "🍽️";
}