import "./ProductToolbar.css";

const SORT_OPTIONS = [
  { value: "relevance", label: "Önerilen Sıralama" },
  { value: "price-asc", label: "Fiyat: Düşükten Yükseğe" },
  { value: "price-desc", label: "Fiyat: Yüksekten Düşüğe" },
  { value: "name-asc", label: "İsim: A - Z" },
];

export default function ProductToolbar({
  title,
  count,
  query,
  onQueryChange,
  brands,
  brand,
  onBrandChange,
  sort,
  onSortChange,
}) {
  return (
    <div className="ptb">
      <div className="ptb__heading">
        <h1 className="ptb__title">{title}</h1>
        <span className="ptb__count">Toplam {count} ürün</span>
      </div>

      <div className="ptb__controls">
        <div className="ptb__search">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <circle cx="11" cy="11" r="7" stroke="currentColor" strokeWidth="1.8" />
            <path d="M20 20L16.5 16.5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
          </svg>
          <input
            type="text"
            placeholder="Ürün adı, marka, stok kodu veya kategori ara..."
            value={query}
            onChange={(e) => onQueryChange(e.target.value)}
          />
          {query && (
            <button
              type="button"
              className="ptb__clear"
              aria-label="Aramayı temizle"
              onClick={() => onQueryChange("")}
            >
              ×
            </button>
          )}
        </div>

        <select
          className="ptb__select"
          value={brand}
          onChange={(e) => onBrandChange(e.target.value)}
          aria-label="Marka filtrele"
        >
          <option value="">Tüm Markalar</option>
          {brands.map((b) => (
            <option key={b} value={b}>
              {b}
            </option>
          ))}
        </select>

        <select
          className="ptb__select"
          value={sort}
          onChange={(e) => onSortChange(e.target.value)}
          aria-label="Sıralama"
        >
          {SORT_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}
