import { useCallback, useEffect, useMemo, useState } from "react";
import { apiFetch } from "../api";
import "./ProductsPage.css";
import "./CustomizationsPage.css";
import ConfirmDialog from "../components/ConfirmDialog";
import { readError, formatRand } from "./adminData";

// NEW: Customizations page.
// Pick a menu item, then manage its option groups (e.g. "Sauce", "Extras")
// and the options inside them (e.g. "Peri-peri +R5").
//   GET    /api/menu-items/{id}/customizations
//   POST   /api/menu-items/{id}/modifier-groups
//   PUT    /api/menu-items/modifier-groups/{groupId}
//   DELETE /api/menu-items/modifier-groups/{groupId}
//   POST   /api/menu-items/modifier-groups/{groupId}/modifiers
//   PUT    /api/menu-items/modifiers/{modifierId}
//   DELETE /api/menu-items/modifiers/{modifierId}
// Options that were bought before can't be deleted (the backend says so):
// hide them instead with the Available switch.

const categoryOf = (item) => item?.category?.categoryName || "Other";

// "Choose 1 (required)", "Choose up to 3 (optional)"...
function describeRule(group) {
  const min = group.minSelections ?? 0;
  const max = group.maxSelections ?? 1;
  const required = min > 0 ? "required" : "optional";
  if (max <= 1) return `Choose 1 · ${required}`;
  if (min > 1) return `Choose ${min} to ${max} · ${required}`;
  return `Choose up to ${max} · ${required}`;
}

// Groups/options from the customizations reply, whatever names they use
function normaliseGroups(data) {
  const groups = data?.modifierGroups || data?.groups || (Array.isArray(data) ? data : []);
  return groups.map((group) => ({
    id: group.modifierGroupId ?? group.id,
    name: group.groupName ?? group.name ?? "Options",
    minSelections: group.minSelections ?? 0,
    maxSelections: group.maxSelections ?? 1,
    options: (group.modifiers || group.options || []).map((option) => ({
      id: option.modifierId ?? option.id,
      name: option.name,
      price: Number(option.priceModifier ?? option.price ?? 0),
      available: (option.isAvailable ?? option.available) !== false,
    })),
  }));
}

async function send(url, method, body) {
  const response = await apiFetch(url, {
    method,
    headers: body ? { "Content-Type": "application/json" } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!response.ok) {
    throw new Error(await readError(response, "That didn't work"));
  }
  return response;
}

// ---------- small forms ----------

function GroupForm({ group, onSave, onCancel }) {
  const [name, setName] = useState(group?.name || "");
  const [min, setMin] = useState(String(group?.minSelections ?? 0));
  const [max, setMax] = useState(String(group?.maxSelections ?? 1));
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    const minValue = Number(min);
    const maxValue = Number(max);

    if (!name.trim()) return setError("Please give the group a name, e.g. Sauce.");
    if (!Number.isInteger(minValue) || minValue < 0)
      return setError("\"At least\" must be 0 or more.");
    if (!Number.isInteger(maxValue) || maxValue < 1)
      return setError("\"At most\" must be 1 or more.");
    if (minValue > maxValue) return setError("\"At least\" can't be more than \"at most\".");

    setSaving(true);
    setError("");
    try {
      await onSave({
        name: name.trim(),
        minSelections: minValue,
        maxSelections: maxValue,
        isRequired: minValue > 0,
        available: true,
      });
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  };

  return (
    <form className="custom-form" onSubmit={submit} noValidate>
      <label className="custom-form-wide">
        Group name
        <input value={name} onChange={(event) => setName(event.target.value)} maxLength={100} placeholder="e.g. Sauce" autoFocus />
      </label>
      <label>
        Customer chooses at least
        <input type="number" min="0" step="1" value={min} onChange={(event) => setMin(event.target.value)} />
        <small>0 = optional</small>
      </label>
      <label>
        and at most
        <input type="number" min="1" step="1" value={max} onChange={(event) => setMax(event.target.value)} />
        <small>1 = pick one</small>
      </label>
      {error && <p className="custom-form-error" role="alert">{error}</p>}
      <div className="custom-form-actions">
        <button type="button" className="products-secondary-button" onClick={onCancel} disabled={saving}>
          Cancel
        </button>
        <button type="submit" className="products-primary-button" disabled={saving}>
          {saving ? "Saving..." : group ? "Save group" : "Add group"}
        </button>
      </div>
    </form>
  );
}

function OptionForm({ option, onSave, onCancel }) {
  const [name, setName] = useState(option?.name || "");
  const [price, setPrice] = useState(option ? String(option.price) : "0");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    const priceValue = Number(price || 0);
    if (!name.trim()) return setError("Please give the option a name, e.g. Peri-peri.");
    if (!Number.isFinite(priceValue) || priceValue < 0)
      return setError("Extra price must be R0 or more.");

    setSaving(true);
    setError("");
    try {
      await onSave({
        name: name.trim(),
        priceModifier: Math.round(priceValue * 100) / 100,
        available: option ? option.available : true,
      });
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  };

  return (
    <form className="custom-form custom-option-form" onSubmit={submit} noValidate>
      <label className="custom-form-wide">
        Option name
        <input value={name} onChange={(event) => setName(event.target.value)} maxLength={100} placeholder="e.g. Peri-peri" autoFocus />
      </label>
      <label>
        Extra price (R)
        <input type="number" min="0" step="0.50" value={price} onChange={(event) => setPrice(event.target.value)} />
        <small>0 = free</small>
      </label>
      {error && <p className="custom-form-error" role="alert">{error}</p>}
      <div className="custom-form-actions">
        <button type="button" className="products-secondary-button" onClick={onCancel} disabled={saving}>
          Cancel
        </button>
        <button type="submit" className="products-primary-button" disabled={saving}>
          {saving ? "Saving..." : option ? "Save option" : "Add option"}
        </button>
      </div>
    </form>
  );
}

// ---------- page ----------

function CustomizationsPage() {
  const [items, setItems] = useState([]);
  const [itemsError, setItemsError] = useState("");
  const [search, setSearch] = useState("");
  const [selectedId, setSelectedId] = useState(null);

  const [groups, setGroups] = useState([]);
  const [loadingGroups, setLoadingGroups] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  // What's open: { type: "newGroup" } | { type: "editGroup", groupId }
  // | { type: "newOption", groupId } | { type: "editOption", optionId }
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null); // { kind, id, name }
  const [busyOptionId, setBusyOptionId] = useState(null);

  // Menu items for the list on the left
  useEffect(() => {
    let cancelled = false;
    apiFetch("/api/menu-items/admin/all")
      .then(async (response) => {
        if (!response.ok) throw new Error(await readError(response, "Could not load the menu"));
        return response.json();
      })
      .then((data) => {
        if (cancelled) return;
        setItems(
          [...data].sort(
            (a, b) =>
              categoryOf(a).localeCompare(categoryOf(b)) ||
              (a.displayOrder ?? 0) - (b.displayOrder ?? 0)
          )
        );
      })
      .catch((err) => {
        if (!cancelled)
          setItemsError(err.message === "Failed to fetch" ? "Cannot reach the server." : err.message);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const loadGroups = useCallback(async (menuItemId) => {
    setLoadingGroups(true);
    try {
      const response = await apiFetch(`/api/menu-items/${menuItemId}/customizations`);
      if (response.status === 404) {
        setGroups([]);
        return;
      }
      if (!response.ok) throw new Error(await readError(response, "Could not load the options"));
      setGroups(normaliseGroups(await response.json()));
    } catch (err) {
      setError(err.message === "Failed to fetch" ? "Cannot reach the server." : err.message);
    } finally {
      setLoadingGroups(false);
    }
  }, []);

  const selectItem = (item) => {
    setSelectedId(item.menuItemId);
    setGroups([]);
    setEditing(null);
    setError("");
    setNotice("");
    loadGroups(item.menuItemId);
  };

  const selected = items.find((item) => item.menuItemId === selectedId) || null;

  const grouped = useMemo(() => {
    const term = search.trim().toLowerCase();
    const map = new Map();
    items
      .filter((item) => !term || `${item.name} ${categoryOf(item)}`.toLowerCase().includes(term))
      .forEach((item) => {
        const name = categoryOf(item);
        if (!map.has(name)) map.set(name, []);
        map.get(name).push(item);
      });
    return [...map.entries()];
  }, [items, search]);

  // Runs a change, then reloads the groups. Form errors are shown in the form.
  const run = async (action, message) => {
    setError("");
    setNotice("");
    await action();
    setEditing(null);
    setNotice(message);
    await loadGroups(selectedId);
  };

  const saveGroup = (body, group) =>
    run(
      () =>
        group
          ? send(`/api/menu-items/modifier-groups/${group.id}`, "PUT", body)
          : send(`/api/menu-items/${selectedId}/modifier-groups`, "POST", body),
      group ? `"${body.name}" saved.` : `"${body.name}" added. Now add its options.`
    );

  const saveOption = (body, groupId, option) =>
    run(
      () =>
        option
          ? send(`/api/menu-items/modifiers/${option.id}`, "PUT", body)
          : send(`/api/menu-items/modifier-groups/${groupId}/modifiers`, "POST", body),
      option ? `"${body.name}" saved.` : `"${body.name}" added.`
    );

  const toggleOption = async (option) => {
    setBusyOptionId(option.id);
    try {
      await run(
        () =>
          send(`/api/menu-items/modifiers/${option.id}`, "PUT", {
            name: option.name,
            priceModifier: option.price,
            available: !option.available,
          }),
        option.available
          ? `"${option.name}" is hidden from students.`
          : `"${option.name}" is available again.`
      );
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyOptionId(null);
    }
  };

  const confirmDelete = async () => {
    const target = deleting;
    setDeleting(null);
    try {
      await run(
        () =>
          send(
            target.kind === "group"
              ? `/api/menu-items/modifier-groups/${target.id}`
              : `/api/menu-items/modifiers/${target.id}`,
            "DELETE"
          ),
        `"${target.name}" deleted.`
      );
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <section className="products-page">
      <div className="products-page-header">
        <div>
          <span className="products-breadcrumb">EDUEATS / MENU MANAGEMENT</span>
          <h2>Customizations</h2>
          <p>Choose an item to manage its options, like sauces and extras.</p>
        </div>
      </div>

      <div className="custom-layout">
        {/* LEFT: menu items */}
        <aside className="custom-items">
          <input
            type="search"
            className="custom-search"
            placeholder="Search menu items..."
            value={search}
            maxLength={50}
            onChange={(event) => setSearch(event.target.value)}
            aria-label="Search menu items"
          />

          {itemsError && <p className="custom-form-error">{itemsError}</p>}

          <div className="custom-item-list">
            {grouped.map(([category, categoryItems]) => (
              <div key={category}>
                <h4>{category}</h4>
                {categoryItems.map((item) => (
                  <button
                    type="button"
                    key={item.menuItemId}
                    className={`custom-item ${item.menuItemId === selectedId ? "active" : ""}`}
                    onClick={() => selectItem(item)}
                  >
                    <span>{item.name}</span>
                    <small>{formatRand(item.price)}</small>
                  </button>
                ))}
              </div>
            ))}
            {!itemsError && items.length > 0 && grouped.length === 0 && (
              <p className="custom-muted">No items match.</p>
            )}
          </div>
        </aside>

        {/* RIGHT: the chosen item's option groups */}
        <div className="custom-editor">
          {!selected ? (
            <div className="custom-empty">
              <span aria-hidden="true">☷</span>
              <p>Choose a menu item on the left to see and change its options.</p>
            </div>
          ) : (
            <>
              <div className="custom-editor-header">
                <div>
                  <h3>{selected.name}</h3>
                  <p>
                    {categoryOf(selected)} · base price {formatRand(selected.price)}
                  </p>
                </div>
                <button
                  type="button"
                  className="products-primary-button"
                  onClick={() => setEditing({ type: "newGroup" })}
                >
                  + Add option group
                </button>
              </div>

              {error && (
                <div className="products-message products-error" role="alert">
                  {error}
                  <button onClick={() => setError("")}>Dismiss</button>
                </div>
              )}
              {notice && (
                <div className="products-message" role="status">
                  {notice}
                  <button onClick={() => setNotice("")}>Dismiss</button>
                </div>
              )}

              {editing?.type === "newGroup" && (
                <div className="custom-group custom-group-new">
                  <h4>New option group</h4>
                  <GroupForm onSave={(body) => saveGroup(body, null)} onCancel={() => setEditing(null)} />
                </div>
              )}

              {loadingGroups && groups.length === 0 && <p className="custom-muted">Loading options...</p>}

              {!loadingGroups && groups.length === 0 && editing?.type !== "newGroup" && (
                <p className="custom-muted">
                  {selected.name} has no options yet, so students add it to the cart in one tap.
                </p>
              )}

              {groups.map((group) => (
                <article className="custom-group" key={group.id}>
                  {editing?.type === "editGroup" && editing.groupId === group.id ? (
                    <>
                      <h4>Edit group</h4>
                      <GroupForm group={group} onSave={(body) => saveGroup(body, group)} onCancel={() => setEditing(null)} />
                    </>
                  ) : (
                    <div className="custom-group-header">
                      <div>
                        <h4>{group.name}</h4>
                        <span className={`custom-rule ${group.minSelections > 0 ? "required" : ""}`}>
                          {describeRule(group)}
                        </span>
                      </div>
                      <div className="custom-actions">
                        <button type="button" onClick={() => setEditing({ type: "editGroup", groupId: group.id })}>
                          Edit
                        </button>
                        <button
                          type="button"
                          className="danger"
                          onClick={() => setDeleting({ kind: "group", id: group.id, name: group.name })}
                        >
                          Delete
                        </button>
                      </div>
                    </div>
                  )}

                  <ul className="custom-options">
                    {group.options.map((option) =>
                      editing?.type === "editOption" && editing.optionId === option.id ? (
                        <li key={option.id} className="custom-option-editing">
                          <OptionForm
                            option={option}
                            onSave={(body) => saveOption(body, group.id, option)}
                            onCancel={() => setEditing(null)}
                          />
                        </li>
                      ) : (
                        <li key={option.id} className={option.available ? "" : "is-hidden"}>
                          <span className="custom-option-name">{option.name}</span>
                          <span className="custom-option-price">
                            {option.price > 0 ? `+${formatRand(option.price)}` : "Free"}
                          </span>
                          <button
                            type="button"
                            className={`custom-switch ${option.available ? "on" : "off"}`}
                            role="switch"
                            aria-checked={option.available}
                            aria-label={`${option.name}: ${option.available ? "available" : "hidden"}`}
                            disabled={busyOptionId === option.id}
                            onClick={() => toggleOption(option)}
                          >
                            <span className="custom-knob" />
                            {option.available ? "Available" : "Hidden"}
                          </button>
                          <div className="custom-actions">
                            <button type="button" onClick={() => setEditing({ type: "editOption", optionId: option.id })}>
                              Edit
                            </button>
                            <button
                              type="button"
                              className="danger"
                              onClick={() => setDeleting({ kind: "option", id: option.id, name: option.name })}
                            >
                              Delete
                            </button>
                          </div>
                        </li>
                      )
                    )}

                    {group.options.length === 0 && (
                      <li className="custom-muted">No options in this group yet.</li>
                    )}
                  </ul>

                  {editing?.type === "newOption" && editing.groupId === group.id ? (
                    <OptionForm onSave={(body) => saveOption(body, group.id, null)} onCancel={() => setEditing(null)} />
                  ) : (
                    <button
                      type="button"
                      className="custom-add-option"
                      onClick={() => setEditing({ type: "newOption", groupId: group.id })}
                    >
                      + Add option
                    </button>
                  )}
                </article>
              ))}
            </>
          )}
        </div>
      </div>

      {deleting && (
        <ConfirmDialog
          title={`Delete ${deleting.name}?`}
          message={
            deleting.kind === "group"
              ? "The group and all its options are removed from this item. If students have bought any of them before, hide the options instead."
              : "It's removed from the menu for good. If students have bought it before, hide it with the Available switch instead."
          }
          confirmLabel="Delete"
          cancelLabel="Keep it"
          danger
          onConfirm={confirmDelete}
          onCancel={() => setDeleting(null)}
        />
      )}
    </section>
  );
}

export default CustomizationsPage;
