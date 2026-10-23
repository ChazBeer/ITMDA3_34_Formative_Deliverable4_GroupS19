import { useCallback, useEffect, useState } from "react";
import { apiFetch } from "../api";
import "./ProductsPage.css";
import "./CategoriesPage.css";
import ConfirmDialog from "../components/ConfirmDialog";
import { readError } from "./adminData";

// NEW: Categories page (full CRUD, admin only for changes).
//   GET    /api/categories        list (with how many items each has)
//   POST   /api/categories        add
//   PUT    /api/categories/{id}   rename / change position
//   DELETE /api/categories/{id}   delete (only when it has no items)
// "View items" opens Products filtered to that category.

const ICONS = [
  ["breakfast", "🍳"],
  ["burger", "🍔"],
  ["toast", "🥪"],
  ["sandwich", "🥪"],
  ["wrap", "🌯"],
  ["hot drink", "☕"],
  ["coffee", "☕"],
  ["drink", "🥤"],
  ["dessert", "🍰"],
  ["snack", "🍟"],
  ["side", "🍟"],
  ["meal", "🍛"],
  ["favourite", "🌭"],
];
const iconFor = (name) =>
  ICONS.find(([word]) => String(name).toLowerCase().includes(word))?.[1] || "🍽️";

function CategoriesPage({ onViewItems }) {
  const [categories, setCategories] = useState([]);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const [name, setName] = useState("");
  const [displayOrder, setDisplayOrder] = useState("");
  const [saving, setSaving] = useState(false);

  // NEW: edit / delete
  const [editingId, setEditingId] = useState(null);
  const [editName, setEditName] = useState("");
  const [editOrder, setEditOrder] = useState("");
  const [deleting, setDeleting] = useState(null);

  // NEW: search and filter
  const [search, setSearch] = useState("");
  const [show, setShow] = useState("all");

  const loadData = useCallback(async () => {
    setError("");
    try {
      const [categoriesResponse, itemsResponse] = await Promise.all([
        apiFetch("/api/categories"),
        apiFetch("/api/menu-items/admin/all"),
      ]);
      if (!categoriesResponse.ok) {
        throw new Error(await readError(categoriesResponse, "Could not load the categories"));
      }
      const categoryList = await categoriesResponse.json();
      setCategories(
        [...categoryList].sort(
          (a, b) =>
            (a.displayOrder ?? 0) - (b.displayOrder ?? 0) ||
            String(a.categoryName).localeCompare(String(b.categoryName))
        )
      );
      // Item counts are extra: the list still shows if this fails
      setItems(itemsResponse.ok ? await itemsResponse.json() : []);
    } catch (err) {
      setError(err.message === "Failed to fetch" ? "Cannot reach the server." : err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const first = setTimeout(loadData, 0);
    return () => clearTimeout(first);
  }, [loadData]);

  const countFor = (category) => {
    const inCategory = items.filter(
      (item) => item.category?.categoryId === category.categoryId
    );
    return {
      total: inCategory.length,
      available: inCategory.filter((item) => item.available !== false).length,
    };
  };

  const addCategory = async (event) => {
    event.preventDefault();
    setError("");
    setNotice("");

    const trimmed = name.trim().replace(/\s+/g, " ");
    if (!trimmed) {
      setError("Please enter a category name.");
      return;
    }
    if (
      categories.some(
        (category) => String(category.categoryName).toLowerCase() === trimmed.toLowerCase()
      )
    ) {
      setError(`There is already a category called "${trimmed}".`);
      return;
    }

    const order =
      displayOrder === ""
        ? Math.max(0, ...categories.map((category) => category.displayOrder ?? 0)) + 1
        : Number(displayOrder);

    if (!Number.isInteger(order) || order < 0) {
      setError("Position must be a whole number of zero or more.");
      return;
    }

    setSaving(true);
    try {
      const response = await apiFetch("/api/categories", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ categoryName: trimmed, displayOrder: order }),
      });
      if (!response.ok) {
        throw new Error(await readError(response, "Could not add the category"));
      }
      setName("");
      setDisplayOrder("");
      setNotice(`"${trimmed}" added. Add items to it on the Products page.`);
      await loadData();
    } catch (err) {
      setError(err.message === "Failed to fetch" ? "Cannot reach the server." : err.message);
    } finally {
      setSaving(false);
    }
  };

  const startEditing = (category) => {
    setEditingId(category.categoryId);
    setEditName(category.categoryName || "");
    setEditOrder(String(category.displayOrder ?? 0));
    setError("");
    setNotice("");
  };

  const saveEdit = async (category) => {
    setError("");
    setNotice("");

    const trimmed = editName.trim().replace(/\s+/g, " ");
    const order = Number(editOrder);
    if (!trimmed) {
      setError("Please enter a category name.");
      return;
    }
    if (
      categories.some(
        (other) =>
          other.categoryId !== category.categoryId &&
          String(other.categoryName).toLowerCase() === trimmed.toLowerCase()
      )
    ) {
      setError(`There is already a category called "${trimmed}".`);
      return;
    }
    if (editOrder === "" || !Number.isInteger(order) || order < 0) {
      setError("Position must be a whole number of zero or more.");
      return;
    }

    setSaving(true);
    try {
      const response = await apiFetch(`/api/categories/${category.categoryId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ categoryName: trimmed, displayOrder: order }),
      });
      if (!response.ok) {
        throw new Error(await readError(response, "Could not save the category"));
      }
      setEditingId(null);
      setNotice(`"${trimmed}" saved.`);
      await loadData();
    } catch (err) {
      setError(err.message === "Failed to fetch" ? "Cannot reach the server." : err.message);
    } finally {
      setSaving(false);
    }
  };

  // Categories that still have items can't be deleted (the items need a category)
  const askDelete = (category) => {
    setError("");
    setNotice("");
    const count = countFor(category).total;
    if (count > 0) {
      setError(
        `"${category.categoryName}" still has ${count} item${count === 1 ? "" : "s"}. ` +
          "Move them to another category or delete them on the Products page first."
      );
      return;
    }
    setDeleting(category);
  };

  const deleteCategory = async () => {
    const category = deleting;
    setDeleting(null);
    try {
      const response = await apiFetch(`/api/categories/${category.categoryId}`, {
        method: "DELETE",
      });
      if (!response.ok) {
        const reason = await readError(response, "Could not delete the category");
        throw new Error(
          response.status === 400 || response.status === 500
            ? `"${category.categoryName}" could not be deleted, it may still be linked to menu items or options.`
            : reason
        );
      }
      setNotice(`"${category.categoryName}" deleted.`);
      await loadData();
    } catch (err) {
      setError(err.message === "Failed to fetch" ? "Cannot reach the server." : err.message);
    }
  };

  // NEW: categories that match the search box and the "Show" filter
  const term = search.trim().toLowerCase();
  const visibleCategories = categories.filter((category) => {
    if (term && !String(category.categoryName).toLowerCase().includes(term)) return false;
    const count = countFor(category);
    if (show === "items") return count.total > 0;
    if (show === "empty") return count.total === 0;
    if (show === "unavailable") return count.total > count.available;
    return true;
  });

  return (
    <section className="products-page">
      <div className="products-page-header">
        <div>
          <span className="products-breadcrumb">EDUEATS / MENU MANAGEMENT</span>
          <h2>Categories</h2>
          <p>The sections of the student menu, in the order students see them.</p>
        </div>
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

      <section className="products-form-container">
        <h3>Add a category</h3>
        <form className="categories-form" onSubmit={addCategory} noValidate>
          <label>
            Category name *
            <input
              value={name}
              onChange={(event) => setName(event.target.value)}
              maxLength={100}
              placeholder="e.g. Smoothies"
            />
          </label>
          <label>
            Position (optional)
            <input
              type="number"
              min="0"
              step="1"
              value={displayOrder}
              onChange={(event) => setDisplayOrder(event.target.value)}
              placeholder="Last"
            />
          </label>
          <button type="submit" className="products-primary-button" disabled={saving}>
            {saving ? "Adding..." : "Add Category"}
          </button>
        </form>
      </section>

      {/* NEW: search and filter */}
      <div className="categories-toolbar">
        <input
          className="products-search"
          type="search"
          placeholder="Search categories..."
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          aria-label="Search categories"
          maxLength={100}
        />
        <select value={show} onChange={(event) => setShow(event.target.value)} aria-label="Show">
          <option value="all">Show: All categories</option>
          <option value="items">With items</option>
          <option value="empty">Empty</option>
          <option value="unavailable">With unavailable items</option>
        </select>
        <span>
          {visibleCategories.length} of {categories.length}
        </span>
      </div>

      {loading ? (
        <div className="products-message">Loading categories...</div>
      ) : (
        <div className="categories-grid">
          {visibleCategories.map((category) => {
            const count = countFor(category);
            return (
              <article className="category-card" key={category.categoryId}>
                <div className="category-card-icon" aria-hidden="true">
                  {iconFor(editingId === category.categoryId ? editName : category.categoryName)}
                </div>

                {editingId === category.categoryId ? (
                  <form
                    className="category-edit"
                    onSubmit={(event) => {
                      event.preventDefault();
                      saveEdit(category);
                    }}
                    noValidate
                  >
                    <input
                      value={editName}
                      onChange={(event) => setEditName(event.target.value)}
                      maxLength={100}
                      aria-label="Category name"
                      autoFocus
                    />
                    <label>
                      Position
                      <input
                        type="number"
                        min="0"
                        step="1"
                        value={editOrder}
                        onChange={(event) => setEditOrder(event.target.value)}
                      />
                    </label>
                    <div className="category-edit-actions">
                      <button
                        type="button"
                        className="products-secondary-button"
                        onClick={() => setEditingId(null)}
                        disabled={saving}
                      >
                        Cancel
                      </button>
                      <button type="submit" className="products-primary-button" disabled={saving}>
                        {saving ? "Saving..." : "Save"}
                      </button>
                    </div>
                  </form>
                ) : (
                  <>
                    <div className="category-card-body">
                      <h3>{category.categoryName}</h3>
                      <p>
                        {count.total} item{count.total === 1 ? "" : "s"}
                        {count.total > count.available &&
                          ` · ${count.total - count.available} unavailable`}
                      </p>
                      <small>Position {category.displayOrder ?? 0}</small>
                      <div className="category-card-actions">
                        <button type="button" onClick={() => onViewItems?.(category.categoryName)}>
                          View items
                        </button>
                        <button type="button" onClick={() => startEditing(category)}>
                          Edit
                        </button>
                        <button type="button" className="danger" onClick={() => askDelete(category)}>
                          Delete
                        </button>
                      </div>
                    </div>
                  </>
                )}
              </article>
            );
          })}

          {categories.length === 0 && (
            <p className="products-message">No categories yet. Add the first one above.</p>
          )}
          {categories.length > 0 && visibleCategories.length === 0 && (
            <p className="products-message">No categories match your search.</p>
          )}
        </div>
      )}
      {deleting && (
        <ConfirmDialog
          title={`Delete ${deleting.categoryName}?`}
          message="It has no items, so nothing else changes. Students won't see this section any more."
          confirmLabel="Delete"
          cancelLabel="Keep it"
          danger
          onConfirm={deleteCategory}
          onCancel={() => setDeleting(null)}
        />
      )}
    </section>
  );
}

export default CategoriesPage;
