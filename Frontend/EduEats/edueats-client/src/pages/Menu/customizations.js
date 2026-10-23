// src/pages/Menu/customizations.js
// Helpers for item customisations (modifier groups like "Sauce" or "Extras").
// Backend: GET /api/menu-items/{menuItemId}/customizations

// Turns the backend's modifier groups into a simple shape for the Customise sheet.
// Works with the current JSON (name, minSelections, priceModifier, id) and also
// with the DTO names (groupName, modifierGroupId, modifierId) in case that changes.
export function normalizeGroups(data, menuItemId) {
  const list = Array.isArray(data) ? data : data?.modifierGroups || [];

  return list
    .filter((group) => group && group.available !== false)
    .map((group) => {
      // display order for THIS menu item (a group can be shared by many items)
      const link = (group.menuItemModifierGroups || []).find(
        (item) =>
          Number(item?.id?.menuItemId ?? item?.menuItemId) === Number(menuItemId)
      );

      const required = Boolean(group.isRequired ?? group.required ?? false);
      const rawMin = Number(group.minSelections ?? group.minimumSelections ?? 0);
      const rawMax = Number(group.maxSelections ?? group.maximumSelections ?? 1);

      // A required group needs at least 1 choice, and max can never be below min
      const min = required ? Math.max(1, rawMin) : Math.max(0, rawMin);
      const max = Math.max(1, rawMax, min);

      const options = (group.modifiers || [])
        .filter(
          (option) =>
            option && option.available !== false && option.isAvailable !== false
        )
        .map((option) => ({
          id: option.id ?? option.modifierId,
          name: option.name,
          price: Number(option.priceModifier ?? option.addedPrice ?? 0),
        }));

      return {
        id: group.id ?? group.modifierGroupId,
        name: group.name ?? group.groupName ?? "Options",
        required,
        min,
        max,
        order: link?.displayOrder ?? 0,
        options,
      };
    })
    .filter((group) => group.options.length > 0)
    .sort((a, b) => a.order - b.order);
}

// Short rule shown under each group name, e.g. "Required · choose 1"
export function describeRule(group) {
  if (group.required) {
    if (group.min === group.max) return `Required · choose ${group.min}`;
    return `Required · choose ${group.min} to ${group.max}`;
  }

  return group.max === 1 ? "Optional · choose 1" : `Optional · up to ${group.max}`;
}

// The same item with the same choices and note becomes ONE cart line;
// different choices become separate lines.
export function makeLineId(menuItemId, modifiers = [], note = "") {
  const ids = modifiers
    .map((modifier) => modifier.id)
    .sort((a, b) => a - b)
    .join(".");

  return `${menuItemId}|${ids}|${note.trim().toLowerCase()}`;
}

// Cart lines saved before customisations existed only had a menuItemId
export const lineIdOf = (line) => line.lineId || String(line.menuItemId);

// Price of ONE of this line: item price + chosen extras
export const unitPrice = (line) =>
  Number(line.price || 0) +
  (line.modifiers || []).reduce((total, modifier) => total + Number(modifier.price || 0), 0);

// "BBQ Sauce, Extra Cheese (+R8.00)" for the cart and confirmation
export const describeModifiers = (modifiers = [], formatRand) =>
  modifiers
    .map((modifier) =>
      modifier.price > 0 ? `${modifier.name} (+${formatRand(modifier.price)})` : modifier.name
    )
    .join(", ");
