import "./ProductThumb.css";

// Ürün görseli varsa küçük thumbnail; yoksa marka baş harflerinden sade
// placeholder. (Mevcut veride image alanları null, bu yüzden placeholder
// kullanılır; ileride image URL'i gelince otomatik gösterilir.)
function initials(text) {
  const t = (text || "").trim();
  if (!t) return "—";
  const words = t.split(/\s+/);
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
  return (words[0][0] + words[1][0]).toUpperCase();
}

export default function ProductThumb({ product, size = 34 }) {
  const style = { width: size, height: size };
  if (product?.image) {
    return (
      <span className="pthumb" style={style}>
        <img src={product.image} alt="" loading="lazy" />
      </span>
    );
  }
  return (
    <span className="pthumb pthumb--ph" style={style} aria-hidden="true">
      {product ? initials(product.brand) : ""}
    </span>
  );
}
