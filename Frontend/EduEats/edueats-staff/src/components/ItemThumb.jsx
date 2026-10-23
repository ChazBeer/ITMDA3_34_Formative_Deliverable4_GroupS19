// src/components/ItemThumb.jsx
// Small square picture of a menu item, or its category emoji
// when no picture has been uploaded (or the picture fails to load).

import { useState } from "react";
import { categoryOf, iconFor, imageUrlOf } from "../menuItems";

function ItemThumb({ item, size = "small" }) {
  const [failedUrl, setFailedUrl] = useState(null);
  const url = imageUrlOf(item);

  if (url && url !== failedUrl) {
    return (
      <img
        className={`item-thumb ${size}`}
        src={url}
        alt=""
        loading="lazy"
        onError={() => setFailedUrl(url)}
      />
    );
  }

  return (
    <span className={`item-thumb placeholder ${size}`} aria-hidden="true">
      {iconFor(categoryOf(item))}
    </span>
  );
}

export default ItemThumb;
