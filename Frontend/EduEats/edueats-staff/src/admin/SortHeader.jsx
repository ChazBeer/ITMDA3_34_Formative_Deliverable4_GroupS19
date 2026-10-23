// src/admin/SortHeader.jsx
// NEW: a table heading you can click to sort by that column.
import "./SortHeader.css";

function SortHeader({ label, column, sort, onSort }) {
  const active = sort.sortKey === column;
  const arrow = !active ? "↕" : sort.direction === "asc" ? "▲" : "▼";

  return (
    <th aria-sort={active ? (sort.direction === "asc" ? "ascending" : "descending") : "none"}>
      <button
        type="button"
        className={`sort-header ${active ? "sort-header-active" : ""}`}
        onClick={() => onSort(column)}
        title={`Sort by ${label.toLowerCase()}`}
      >
        {label}
        <span className="sort-arrow" aria-hidden="true">
          {arrow}
        </span>
      </button>
    </th>
  );
}

export default SortHeader;
