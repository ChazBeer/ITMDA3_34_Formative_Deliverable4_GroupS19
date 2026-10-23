// src/components/CategoryBar.jsx
// The row of category buttons (All, Burgers, Breakfast...).
// It scrolls sideways, so to make that obvious:
// - the edges fade out when there are more buttons that way
// - round ‹ › arrow buttons appear on the side that has more
// - the first time the menu opens, the row gives a small "nudge"
// - the chosen category always scrolls into view

import { useEffect, useRef, useState } from "react";
import "./CategoryBar.css";

// The nudge plays once per browser tab
const NUDGE_KEY = "edueatsCategoryNudge";

function CategoryBar({ categories, selected, onSelect }) {
  const rowRef = useRef(null);
  const [edges, setEdges] = useState({ left: false, right: false });

  // Work out if there is more to see on the left / right
  useEffect(() => {
    const row = rowRef.current;
    if (!row) return undefined;

    const measure = () => {
      const maxScroll = row.scrollWidth - row.clientWidth;
      setEdges({
        left: row.scrollLeft > 4,
        right: row.scrollLeft < maxScroll - 4,
      });
    };

    const frame = requestAnimationFrame(measure);
    row.addEventListener("scroll", measure, { passive: true });
    window.addEventListener("resize", measure);

    return () => {
      cancelAnimationFrame(frame);
      row.removeEventListener("scroll", measure);
      window.removeEventListener("resize", measure);
    };
  }, [categories]);

  // A small one-time nudge so students notice the row can move
  useEffect(() => {
    const row = rowRef.current;
    if (!row) return undefined;

    let alreadyNudged = false;
    try {
      alreadyNudged = sessionStorage.getItem(NUDGE_KEY) === "1";
    } catch {
      // storage blocked - just nudge
    }

    const reduceMotion = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
    if (alreadyNudged || reduceMotion) return undefined;

    const start = setTimeout(() => {
      if (row.scrollWidth <= row.clientWidth + 4) return; // nothing to scroll
      row.scrollBy({ left: 60, behavior: "smooth" });
      try {
        sessionStorage.setItem(NUDGE_KEY, "1");
      } catch {
        // not saved - the nudge may play again next time
      }
    }, 700);

    const back = setTimeout(() => {
      row.scrollBy({ left: -60, behavior: "smooth" });
    }, 1300);

    return () => {
      clearTimeout(start);
      clearTimeout(back);
    };
  }, [categories]);

  // Keep the chosen category visible
  useEffect(() => {
    const row = rowRef.current;
    const active = row?.querySelector("button[aria-selected='true']");
    active?.scrollIntoView?.({ behavior: "smooth", block: "nearest", inline: "nearest" });
  }, [selected]);

  const scrollByPage = (direction) => {
    const row = rowRef.current;
    if (!row) return;
    row.scrollBy({ left: direction * row.clientWidth * 0.7, behavior: "smooth" });
  };

  return (
    <div
      className={`category-bar ${edges.left ? "more-left" : ""} ${edges.right ? "more-right" : ""}`}
    >
      {edges.left && (
        <button
          type="button"
          className="category-arrow left"
          onClick={() => scrollByPage(-1)}
          aria-label="Show earlier categories"
          tabIndex={-1}
        >
          ‹
        </button>
      )}

      <div className="category-buttons" role="tablist" ref={rowRef}>
        {categories.map((category) => (
          <button
            key={category}
            type="button"
            role="tab"
            aria-selected={category === selected}
            className={category === selected ? "active" : ""}
            onClick={() => onSelect(category)}
          >
            {category}
          </button>
        ))}
      </div>

      {edges.right && (
        <button
          type="button"
          className="category-arrow right"
          onClick={() => scrollByPage(1)}
          aria-label="Show more categories"
          tabIndex={-1}
        >
          ›
        </button>
      )}
    </div>
  );
}

export default CategoryBar;
