// src/customizations.js
// Item options ("Sauce", "Extras"...) for manual orders.
// Same rules as the student app: GET /api/menu-items/{id}/customizations

// Turns the backend's option groups into a simple shape
export function normalizeGroups(data) {
  const list = Array.isArray(data) ? data : data?.modifierGroups || [];

  return list
    .filter((group) => group && group.available !== false)
    .map((group) => {
      const required = Boolean(group.isRequired ?? group.required ?? false);
      const rawMin = Number(group.minSelections ?? group.minimumSelections ?? 0);
      const rawMax = Number(group.maxSelections ?? group.maximumSelections ?? 1);
      const min = required ? Math.max(1, rawMin) : Math.max(0, rawMin);
      const max = Math.max(1, rawMax, min);

      const options = (group.modifiers || [])
        .filter((option) => option && option.available !== false && option.isAvailable !== false)
        .map((option) => ({
          id: option.id ?? option.modifierId,
          name: option.name,
          price: Number(option.priceModifier ?? option.addedPrice ?? 0),
        }));

      return {
        id: group.id ?? group.modifierGroupId,
        name: group.name ?? group.groupName ?? "Options",
        min,
        max,
        options,
      };
    })
    .filter((group) => group.options.length > 0);
}

// e.g. "Required · choose 1" or "Optional · up to 3"
export function describeRule(group) {
  if (group.min > 0) {
    return group.min === group.max
      ? `Required · choose ${group.min}`
      : `Required · choose ${group.min} to ${group.max}`;
  }
  return group.max === 1 ? "Optional · choose 1" : `Optional · up to ${group.max}`;
}

// Price of ONE of a line: item price + chosen extras
export const unitPrice = (line) =>
  Number(line.item.price || 0) +
  line.modifiers.reduce((total, modifier) => total + Number(modifier.price || 0), 0);
