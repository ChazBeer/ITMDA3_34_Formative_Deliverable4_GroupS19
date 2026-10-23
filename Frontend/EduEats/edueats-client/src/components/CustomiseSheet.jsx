// src/components/CustomiseSheet.jsx
// Pop-up where a student picks options for an item before adding it to the cart
// (e.g. Sauce: choose 1, Extras: up to 3) plus a note for the kitchen.
// Phone: slides up from the bottom. Laptop: centred box.

import { useEffect, useMemo, useRef, useState } from "react";
import "./CustomiseSheet.css";
import { describeRule } from "../pages/Menu/customizations";

const NOTE_MAX_LENGTH = 150;

function CustomiseSheet({ item, groups, maxQuantity = 10, formatRand, onAdd, onClose }) {
  // { groupId: [optionId, ...] }
  const [selected, setSelected] = useState({});
  const [note, setNote] = useState("");
  const [quantity, setQuantity] = useState(1);
  const [showErrors, setShowErrors] = useState(false);
  const groupRefs = useRef({});

  // Close with Escape, and stop the page behind from scrolling
  useEffect(() => {
    const onKey = (event) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";

    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [onClose]);

  const chosenIn = (group) => selected[group.id] || [];

  // Pick an option. One-choice groups behave like radio buttons,
  // the others like checkboxes (up to the group's maximum).
  const toggleOption = (group, option) => {
    setSelected((current) => {
      const chosen = current[group.id] || [];
      const isChosen = chosen.includes(option.id);

      if (group.max === 1) {
        // tapping the chosen option again clears it (only if the group is optional)
        if (isChosen && !group.required) return { ...current, [group.id]: [] };
        return { ...current, [group.id]: [option.id] };
      }

      if (isChosen) {
        return { ...current, [group.id]: chosen.filter((id) => id !== option.id) };
      }

      if (chosen.length >= group.max) return current; // already at the limit
      return { ...current, [group.id]: [...chosen, option.id] };
    });
  };

  // Groups that still need more choices
  const missingGroups = groups.filter((group) => chosenIn(group).length < group.min);

  // Everything the student picked, with names and prices (saved in the cart)
  const chosenModifiers = useMemo(
    () =>
      groups.flatMap((group) =>
        group.options.filter((option) => (selected[group.id] || []).includes(option.id))
      ),
    [groups, selected]
  );

  const extrasPrice = chosenModifiers.reduce((total, option) => total + option.price, 0);
  const totalPrice = (Number(item.price) + extrasPrice) * quantity;

  const handleAdd = () => {
    if (missingGroups.length > 0) {
      setShowErrors(true);
      // scroll to the first group that still needs a choice
      groupRefs.current[missingGroups[0].id]?.scrollIntoView({
        behavior: "smooth",
        block: "center",
      });
      return;
    }

    onAdd({ modifiers: chosenModifiers, note: note.trim(), quantity });
  };

  return (
    <div className="sheet-overlay" onClick={onClose}>
      <div
        className="sheet"
        role="dialog"
        aria-modal="true"
        aria-labelledby="sheet-title"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="sheet-header">
          <div>
            <h2 id="sheet-title">{item.name}</h2>
            <p>{formatRand(item.price)}</p>
          </div>

          <button type="button" className="sheet-close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </header>

        <div className="sheet-body">
          {groups.map((group) => {
            const chosen = chosenIn(group);
            const missing = showErrors && chosen.length < group.min;
            const atLimit = group.max > 1 && chosen.length >= group.max;
            const inputType = group.max === 1 ? "radio" : "checkbox";

            return (
              <fieldset
                key={group.id}
                className={`sheet-group ${missing ? "missing" : ""}`}
                ref={(element) => {
                  groupRefs.current[group.id] = element;
                }}
              >
                <legend>
                  <strong>{group.name}</strong>
                  <span className={group.required ? "rule required" : "rule"}>
                    {describeRule(group)}
                  </span>
                </legend>

                {missing && (
                  <p className="sheet-error" role="alert">
                    Please choose {group.min === 1 ? "one" : `at least ${group.min}`}.
                  </p>
                )}

                {group.options.map((option) => {
                  const isChosen = chosen.includes(option.id);
                  const disabled = !isChosen && atLimit;

                  return (
                    <label
                      key={option.id}
                      className={`sheet-option ${isChosen ? "chosen" : ""} ${
                        disabled ? "disabled" : ""
                      }`}
                    >
                      <input
                        type={inputType}
                        name={`group-${group.id}`}
                        checked={isChosen}
                        disabled={disabled}
                        onChange={() => toggleOption(group, option)}
                        onClick={() => {
                          // lets a chosen radio be un-ticked in optional groups
                          if (inputType === "radio" && isChosen && !group.required) {
                            toggleOption(group, option);
                          }
                        }}
                      />
                      <span className="sheet-option-name">{option.name}</span>
                      <span className="sheet-option-price">
                        {option.price > 0 ? `+${formatRand(option.price)}` : ""}
                      </span>
                    </label>
                  );
                })}
              </fieldset>
            );
          })}

          <label className="sheet-note">
            <strong>Note for the kitchen</strong>
            <span className="rule">Optional</span>
            <textarea
              rows={2}
              maxLength={NOTE_MAX_LENGTH}
              placeholder="e.g. no onions, extra crispy"
              value={note}
              onChange={(event) => setNote(event.target.value)}
            />
            <small>
              {note.length}/{NOTE_MAX_LENGTH}
            </small>
          </label>
        </div>

        <footer className="sheet-footer">
          <div className="sheet-qty" aria-label="Quantity">
            <button
              type="button"
              onClick={() => setQuantity((q) => Math.max(1, q - 1))}
              disabled={quantity <= 1}
              aria-label="One less"
            >
              −
            </button>
            <span>{quantity}</span>
            <button
              type="button"
              onClick={() => setQuantity((q) => Math.min(maxQuantity, q + 1))}
              disabled={quantity >= maxQuantity}
              aria-label="One more"
            >
              +
            </button>
          </div>

          <button
            type="button"
            className={`sheet-add ${missingGroups.length > 0 ? "incomplete" : ""}`}
            onClick={handleAdd}
          >
            Add to cart · {formatRand(totalPrice)}
          </button>
        </footer>
      </div>
    </div>
  );
}

export default CustomiseSheet;
