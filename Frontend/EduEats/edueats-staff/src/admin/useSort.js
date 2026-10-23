// src/admin/useSort.js
// NEW: click-to-sort for the admin tables (Users, Staff, Admins).
// Click a column heading once for A→Z (dates: newest first), again to flip it.
// Empty values ("Never", "—") always stay at the bottom.

import { useState } from "react";

const isEmpty = (value) => value === null || value === undefined || value === "" || value === "—";

function compareValues(a, b) {
  if (typeof a === "number" && typeof b === "number") return a - b;
  return String(a).localeCompare(String(b), "en", { sensitivity: "base", numeric: true });
}

// newestFirst: date columns that start with the newest at the top
export function useSort(initialKey = "name", initialDirection = "asc", newestFirst = ["joined", "lastLogin"]) {
  const [sortKey, setSortKey] = useState(initialKey);
  const [direction, setDirection] = useState(initialDirection);

  // Same column again flips the order; a new column starts at A→Z / newest first
  const toggle = (key) => {
    if (key === sortKey) {
      setDirection((current) => (current === "asc" ? "desc" : "asc"));
    } else {
      setSortKey(key);
      setDirection(newestFirst.includes(key) ? "desc" : "asc");
    }
  };

  // valueOf maps a column key to the value to sort on for one row
  const sortRows = (rows, valueOf) => {
    if (!sortKey) return rows; // no column chosen yet: keep the original order
    const sign = direction === "asc" ? 1 : -1;
    return [...rows].sort((rowA, rowB) => {
      const a = valueOf(rowA, sortKey);
      const b = valueOf(rowB, sortKey);
      if (isEmpty(a) && isEmpty(b)) return 0;
      if (isEmpty(a)) return 1;
      if (isEmpty(b)) return -1;
      return compareValues(a, b) * sign;
    });
  };

  return { sortKey, direction, toggle, sortRows };
}

// Dates as numbers, so newest/oldest sort correctly
export const timeOf = (value) => {
  const time = value ? new Date(value).getTime() : NaN;
  return Number.isNaN(time) ? null : time;
};
