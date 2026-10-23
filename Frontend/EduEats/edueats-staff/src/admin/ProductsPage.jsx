
import { useCallback, useEffect, useState } from "react";
import { apiFetch } from "../api";
import "./ProductsPage.css";
import ConfirmDialog from "../components/ConfirmDialog";
import { readError } from "./adminData";
import SortHeader from "./SortHeader";
import { useSort } from "./useSort";

const emptyForm = {
  name: "",
  description: "",
  price: "",
  categoryId: "",
  available: true,
  displayOrder: 0,
  stockQuantity: "",
};

// NEW: stock. "Low" from this number down.
const LOW_STOCK = 5;
const stockOf = (product) =>
  product?.stockQuantity === undefined || product?.stockQuantity === null
    ? null
    : Number(product.stockQuantity);

// NEW: the full update (PUT) replaces every field, so changes like
// "mark unavailable" or "set stock" send the item's other values as they are.
// (Without stockQuantity the backend would reset the stock to 0.)
function toPayload(product, changes = {}) {
  return {
    name: product.name,
    description: product.description || "",
    price: Number(product.price),
    available: product.available !== false,
    displayOrder: product.displayOrder ?? 0,
    stockQuantity: stockOf(product) ?? 0,
    category: { categoryId: product.category?.categoryId },
    ...changes,
  };
}

// NEW: stock number you can change right in the table (Enter or click away saves)
function StockCell({ product, onSave }) {
  const stock = stockOf(product);
  const [draft, setDraft] = useState(stock === null ? "" : String(stock));
  const [saving, setSaving] = useState(false);

  const save = async () => {
    const value = Number(draft);
    if (draft === "" || !Number.isInteger(value) || value < 0) {
      setDraft(stock === null ? "" : String(stock));
      return;
    }
    if (value === stock) return;
    setSaving(true);
    const ok = await onSave(product, value);
    setSaving(false);
    if (!ok) setDraft(stock === null ? "" : String(stock));
  };

  const level = stock === null ? "" : stock === 0 ? "out" : stock <= LOW_STOCK ? "low" : "ok";

  return (
    <div className={`products-stock products-stock-${level}`}>
      <input
        type="number"
        min="0"
        step="1"
        inputMode="numeric"
        value={draft}
        disabled={saving}
        aria-label={`Stock for ${product.name}`}
        onChange={(event) => setDraft(event.target.value)}
        onBlur={save}
        onKeyDown={(event) => {
          if (event.key === "Enter") event.currentTarget.blur();
        }}
      />
      <small>
        {saving ? "Saving..." : level === "out" ? "Sold out" : level === "low" ? "Low" : "in stock"}
      </small>
    </div>
  );
}

function ProductsPage({ initialSearch = "" }) {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [formOpen, setFormOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [search, setSearch] = useState(initialSearch);
  const [deleting, setDeleting] = useState(null); // product waiting for "are you sure?"
  // NEW: click a column heading to sort (menu order until a heading is clicked)
  const sort = useSort(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const [productsResponse, categoriesResponse] = await Promise.all([
        apiFetch("/api/menu-items/admin/all"),
        apiFetch("/api/categories"),
      ]);

      if (!productsResponse.ok || !categoriesResponse.ok) {
        throw new Error("Could not load menu items or categories.");
      }

      const [productsData, categoriesData] = await Promise.all([
        productsResponse.json(),
        categoriesResponse.json(),
      ]);

      setProducts(productsData);
      setCategories(categoriesData);
    } catch (err) {
      setError(err.message || "Could not load the menu.");
    } finally {
      setLoading(false);
    }
  }, []);

  // First load straight after the page appears
  useEffect(() => {
    const first = setTimeout(loadData, 0);
    return () => clearTimeout(first);
  }, [loadData]);

  function updateForm(event) {
    const { name, value, type, checked } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]: type === "checkbox" ? checked : value,
    }));
  }

  function startEditing(product) {
    setFormOpen(true);
    setEditingId(product.menuItemId);
    setForm({
      name: product.name || "",
      description: product.description || "",
      price: String(product.price ?? ""),
      categoryId: String(product.category?.categoryId ?? ""),
      available: product.available ?? true,
      displayOrder: product.displayOrder ?? 0,
      stockQuantity: String(stockOf(product) ?? 0),
    });
    setNotice("");
    setError("");
  }

  function cancelEditing() {
    setEditingId(null);
    setForm(emptyForm);
    setFormOpen(false);
  }

  async function saveProduct(event) {
    event.preventDefault();
    setError("");
    setNotice("");

    if (!form.categoryId) {
      setError("Please select a category.");
      return;
    }

    const price = Number(form.price);

    if (!Number.isFinite(price) || price < 0) {
      setError("Enter a valid price of zero or more.");
      return;
    }

    const stockQuantity = form.stockQuantity === "" ? 0 : Number(form.stockQuantity);

    if (!Number.isInteger(stockQuantity) || stockQuantity < 0) {
      setError("Enter a whole number of zero or more for the stock.");
      return;
    }

    const payload = {
      name: form.name.trim(),
      description: form.description.trim(),
      price,
      available: form.available,
      displayOrder: Number(form.displayOrder) || 0,
      stockQuantity,
      category: { categoryId: Number(form.categoryId) },
    };

    if (!payload.name) {
      setError("Product name is required.");
      return;
    }

    setSaving(true);

    try {
      const url = editingId
        ? `/api/menu-items/${editingId}`
        : `/api/menu-items/category/${form.categoryId}`;

      const response = await apiFetch(url, {
        method: editingId ? "PUT" : "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        throw new Error(
          await readError(response, `Could not ${editingId ? "update" : "create"} the product`)
        );
      }

      cancelEditing();
      setNotice(editingId ? "Product updated." : "Product created.");
      await loadData();
    } catch (err) {
      setError(err.message || "Could not save the product.");
    } finally {
      setSaving(false);
    }
  }

  async function toggleAvailability(product) {
    setError("");
    setNotice("");

    const payload = toPayload(product, { available: !product.available });

    try {
      // NEW: the availability-only endpoint (changes nothing else)
      let response = await apiFetch(
        `/api/menu-items/${product.menuItemId}/availability?available=${payload.available}`,
        { method: "PATCH" }
      );

      // Older backend without it: use the full update instead
      if (response.status === 404 || response.status === 405) {
        response = await apiFetch(`/api/menu-items/${product.menuItemId}`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        });
      }

      if (!response.ok) {
        throw new Error(await readError(response, "Could not change availability"));
      }

      setNotice(
        `${product.name} marked ${
          payload.available ? "available" : "unavailable"
        }.`
      );
      await loadData();
    } catch (err) {
      setError(err.message || "Could not change availability.");
    }
  }

  // NEW: set the stock straight from the table. Returns true if saved.
  // The backend marks an item unavailable when its stock reaches 0.
  // Restocking does NOT switch it back on: an admin decides when it's ready
  // to sell (with "Mark available"), as agreed with the backend team.
  async function saveStock(product, stockQuantity) {
    setError("");
    setNotice("");

    const changes = { stockQuantity };

    try {
      const response = await apiFetch(`/api/menu-items/${product.menuItemId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(toPayload(product, changes)),
      });

      if (!response.ok) {
        throw new Error(await readError(response, "Could not save the stock"));
      }

      // Stock 0 = the backend switches the item off
      const available = stockQuantity === 0 ? false : product.available;
      setProducts((list) =>
        list.map((item) =>
          item.menuItemId === product.menuItemId ? { ...item, stockQuantity, available } : item
        )
      );
      setNotice(
        stockQuantity === 0
          ? `${product.name}: 0 in stock, so it's now unavailable to students.`
          : product.available === false
            ? `${product.name}: ${stockQuantity} in stock, but still unavailable. Click "Mark available" when it's ready to sell.`
            : `${product.name}: ${stockQuantity} in stock.`
      );
      return true;
    } catch (err) {
      setError(err.message || "Could not save the stock.");
      return false;
    }
  }

  async function deleteProduct(product) {
    setDeleting(null);
    setError("");
    setNotice("");

    try {
      const response = await apiFetch(`/api/menu-items/${product.menuItemId}`, {
        method: "DELETE",
      });

      if (!response.ok) {
        const reason = await readError(response, "Could not delete the product");
        throw new Error(
          response.status === 400 || response.status === 500
            ? `${reason} If it has been ordered before, mark it unavailable instead.`
            : reason
        );
      }

      if (editingId === product.menuItemId) {
        cancelEditing();
      }

      setNotice("Product deleted.");
      await loadData();
    } catch (err) {
      setError(err.message || "Could not delete the product.");
    }
  }

  async function uploadImage(product, file) {
    if (!file) return;

    setError("");
    setNotice("");

    const data = new FormData();
    data.append("file", file);

    try {
      const response = await apiFetch(
        `/api/menu-items/${product.menuItemId}/image`,
        { method: "POST", body: data }
      );

      if (!response.ok) {
        const details = await response.text();

        throw new Error(
            `Could not upload photo (${response.status}): ${
            details || "No additional details provided."
            }`
        );
        }

      setNotice(`Photo uploaded for ${product.name}.`);
      await loadData();
    } catch (err) {
      setError(err.message || "Could not upload the photo.");
    }
  }

  const lowStockCount = products.filter((product) => {
    const stock = stockOf(product);
    return product.available !== false && stock !== null && stock <= LOW_STOCK;
  }).length;

  const filteredProducts = products.filter((product) => {
    const query = search.toLowerCase();

    return (
      product.name?.toLowerCase().includes(query) ||
      product.category?.categoryName?.toLowerCase().includes(query)
    );
  });

  // NEW: sorting by name, category, price, stock or availability
  const sortedProducts = sort.sortRows(filteredProducts, (product, column) => {
    if (column === "name") return product.name || "";
    if (column === "category") return product.category?.categoryName || "";
    if (column === "price") return Number(product.price || 0);
    if (column === "stock") return stockOf(product);
    if (column === "available") return product.available === false ? 1 : 0;
    return "";
  });

  return (
    <section className="products-page">
      <div className="products-page-header">
        <div>
          <span className="products-breadcrumb">
            EDUEATS / MENU MANAGEMENT
          </span>
          <h2>Products &amp; Menu</h2>
          <p>Manage menu items, prices, photos and availability.</p>
        </div>

        <button
          className="products-primary-button"
          onClick={() => {
            cancelEditing();
            setFormOpen(true);
            setError("");
            setNotice("");
            document.getElementById("product-form")?.scrollIntoView({
                behavior: "smooth",
                block: "start",
            });
        }}
        >
          + Add Product
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
    {formOpen && (
        <section className="products-form-container" id="product-form">
        <h3>{editingId ? "Edit Product" : "Add a Product"}</h3>

        <form className="products-form" onSubmit={saveProduct}>
          <label>
            Product name *
            <input
              name="name"
              value={form.name}
              onChange={updateForm}
              maxLength={150}
              required
            />
          </label>

          <label>
            Category *
            <select
              name="categoryId"
              value={form.categoryId}
              onChange={updateForm}
              required
            >
              <option value="">Select a category</option>
              {categories.map((category) => (
                <option key={category.categoryId} value={category.categoryId}>
                  {category.categoryName}
                </option>
              ))}
            </select>
          </label>

          <label>
            Price (R) *
            <input
              name="price"
              type="number"
              min="0"
              step="0.01"
              value={form.price}
              onChange={updateForm}
              required
            />
          </label>

          <label>
            Display order
            <input
              name="displayOrder"
              type="number"
              step="1"
              value={form.displayOrder}
              onChange={updateForm}
            />
          </label>

          <label>
            Stock (how many can be ordered)
            <input
              name="stockQuantity"
              type="number"
              min="0"
              step="1"
              value={form.stockQuantity}
              onChange={updateForm}
              placeholder="0"
            />
          </label>

          <label className="products-description-field">
            Description
            <textarea
              name="description"
              value={form.description}
              onChange={updateForm}
              rows="3"
            />
          </label>

          <label className="products-checkbox-field">
            <input
              name="available"
              type="checkbox"
              checked={form.available}
              onChange={updateForm}
            />
            Available for ordering
          </label>

          <div className="products-form-actions">
            <button
              type="submit"
              className="products-primary-button"
              disabled={saving}
            >
              {saving
                ? "Saving..."
                : editingId
                  ? "Save Changes"
                  : "Create Product"}
            </button>

            <button
            type="button"
            className="products-secondary-button"
            onClick={cancelEditing}
            disabled={saving}
            >
            Cancel
            </button>
          </div>
        </form>
      </section>
    )}

      <section className="products-table-container">
        <div className="products-summary">
          <span>{products.length} menu items</span>
          <span>{categories.length} active categories</span>
          {lowStockCount > 0 && (
            <span className="products-summary-warning">{lowStockCount} low or sold out</span>
          )}
          <input
            className="products-search"
            type="search"
            placeholder="Search products or categories..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            aria-label="Search products or categories"
          />
        </div>

        {loading ? (
          <div className="products-message">Loading menu items...</div>
        ) : (
          <div className="products-table-scroll">
            <table className="products-table">
              <thead>
                <tr>
                  <SortHeader label="Product" column="name" sort={sort} onSort={sort.toggle} />
                  <SortHeader label="Category" column="category" sort={sort} onSort={sort.toggle} />
                  <SortHeader label="Price" column="price" sort={sort} onSort={sort.toggle} />
                  <SortHeader label="Stock" column="stock" sort={sort} onSort={sort.toggle} />
                  <SortHeader label="Availability" column="available" sort={sort} onSort={sort.toggle} />
                  <th>Photo</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {sortedProducts.length === 0 ? (
                  <tr>
                    <td colSpan="7">No matching menu items found.</td>
                  </tr>
                ) : (
                  sortedProducts.map((product) => (
                    <tr key={product.menuItemId}>
                      <td>
                        <strong>{product.name}</strong>
                        <small>
                          {product.description || "No description"}
                        </small>
                      </td>
                      <td>
                        {product.category?.categoryName || "Uncategorised"}
                      </td>
                      <td>R {Number(product.price || 0).toFixed(2)}</td>
                      <td>
                        <StockCell
                          key={`${product.menuItemId}-${product.stockQuantity}`}
                          product={product}
                          onSave={saveStock}
                        />
                      </td>
                      <td>
                        <span
                          className={`products-status ${
                            product.available
                              ? "products-status-available"
                              : "products-status-unavailable"
                          }`}
                        >
                          {product.available ? "Available" : "Unavailable"}
                        </span>
                      </td>
                      <td>
                        {product.imagePath && (
                          <a
                            href={product.imagePath}
                            target="_blank"
                            rel="noreferrer"
                          >
                            View photo
                          </a>
                        )}
                        <label className="products-upload-label">
                          {product.imagePath ? "Replace" : "Upload"}
                          <input
                            type="file"
                            accept="image/*"
                            onChange={(event) => {
                              const file = event.target.files?.[0];
                              uploadImage(product, file);
                              event.target.value = "";
                            }}
                          />
                        </label>
                      </td>
                      <td>
                        <div className="products-row-actions">
                          <button
                            type="button"
                            onClick={() => startEditing(product)}
                          >
                            Edit
                          </button>
                          <button
                            type="button"
                            onClick={() => toggleAvailability(product)}
                          >
                            {product.available ? "Mark unavailable" : "Mark available"}
                          </button>
                          <button
                            type="button"
                            className="products-delete-button"
                            onClick={() => setDeleting(product)}
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {deleting && (
        <ConfirmDialog
          title={`Delete ${deleting.name}?`}
          message="It disappears from the menu for good. If students have ordered it before, mark it unavailable instead."
          confirmLabel="Delete"
          cancelLabel="Keep it"
          danger
          onConfirm={() => deleteProduct(deleting)}
          onCancel={() => setDeleting(null)}
        />
      )}
    </section>
  );
}

export default ProductsPage;
