import { useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { products } from "../data/products";
import { categories } from "../data/categories";
import QuoteRow from "../components/QuoteRow";
import QuoteSummary from "../components/QuoteSummary";
import QuoteWarning from "../components/QuoteWarning";
import "./QuoteBuilderPage.css";
import { useCart } from "../context/CartContext";

// Ürün stok durumu: stok alanı yoksa "stokta" say (mevcut davranışı bozma).
function isInStock(p) {
  if (p.stock != null) return Number(p.stock) > 0;
  if (p.inStock != null) return p.inStock === true;
  return true;
}

// id → ürün eşlemesi (tek sefer)
const productById = new Map(products.map((p) => [p.id, p]));

// Düz alt kategori listesi: "Ana › Alt", değer = categorySlug. Sıra korunur.
const categoryOptions = categories.flatMap((main) =>
  main.subCategories.map((sub) => ({
    value: sub.slug,
    label: `${main.name} › ${sub.name}`,
  })),
);

const newRow = (uid) => ({ uid, categorySlug: "", productId: null, qty: 1 });

export default function QuoteBuilderPage() {
  const { addItem } = useCart();
  const uidRef = useRef(6);
  const [rows, setRows] = useState(() =>
    Array.from({ length: 6 }, (_, i) => newRow(i)),
  );
  const [onlyInStock, setOnlyInStock] = useState(false);

  const patchRow = (uid, patch) =>
    setRows((prev) =>
      prev.map((r) => (r.uid === uid ? { ...r, ...patch } : r)),
    );

  const handleCategoryChange = (uid, slug) =>
    // Kategori değişince ürün seçimi sıfırlanır (yeni kategoriye ait değil).
    patchRow(uid, { categorySlug: slug, productId: null });

  const handleProductChange = (uid, id) => patchRow(uid, { productId: id });
  const handleQtyChange = (uid, qty) => patchRow(uid, { qty });

  const handleRemove = (uid) =>
    setRows((prev) => {
      const next = prev.filter((r) => r.uid !== uid);
      return next.length ? next : [newRow(uidRef.current++)];
    });

  const addRow = () => setRows((prev) => [...prev, newRow(uidRef.current++)]);

  const clearAll = () => {
    const hasSelection = rows.some((r) => r.productId);
    if (hasSelection && !window.confirm("Seçilen tüm ürünler temizlensin mi?"))
      return;
    setRows([newRow(uidRef.current++)]);
  };
  const handleAddToCart = () => {
    const selectedRows = rows.filter((row) => row.productId != null);

    if (selectedRows.length === 0) {
      alert("Lütfen sepete eklemek için en az bir ürün seçiniz.");
      return;
    }

    selectedRows.forEach((row) => {
      const product = productById.get(row.productId);

      if (product) {
        addItem(product, row.qty);
      }
    });
  };

  // Satır bazında seçilebilir ürün listesi (kategori + stok filtresi)
  const productsForCategory = (slug) => {
    if (!slug) return [];
    return products.filter(
      (p) => p.categorySlug === slug && (!onlyInStock || isInStock(p)),
    );
  };

  // Para birimi bazında ara toplam (KDV öncesi). Farklı döviz toplanmaz.
  const totalsByCurrency = useMemo(() => {
    const totals = {};
    for (const r of rows) {
      const p = r.productId != null ? productById.get(r.productId) : null;
      if (!p || p.price == null) continue;
      const cur = p.currency || "USD";
      totals[cur] = (totals[cur] || 0) + p.price * r.qty;
    }
    return totals;
  }, [rows]);

  return (
    <div className="qbp">
      <div className="qbp__inner">
        <nav className="qbp__breadcrumb" aria-label="Breadcrumb">
          <Link to="/">Anasayfa</Link>
          <span aria-hidden="true">/</span>
          <span aria-current="page">Teklif Sihirbazı</span>
        </nav>

        <div className="qbp__actions">
          <button type="button" className="qbp__clear" onClick={clearAll}>
            ⟳ Temizle
          </button>
          <button
            type="button"
            className={"qbp__stock" + (onlyInStock ? " qbp__stock--on" : "")}
            onClick={() => setOnlyInStock((v) => !v)}
            aria-pressed={onlyInStock}
          >
            <span className="qbp__stock-box" aria-hidden="true">
              {onlyInStock ? "✓" : ""}
            </span>
            Stoktaki Ürünler
          </button>
        </div>

        <div className="qbp__layout">
          <section className="qbp__table" aria-label="Ürün tablosu">
            <div className="qbp__table-scroll">
              <div className="qrow qrow--head" aria-hidden="true">
                <div className="qrow__cat">Kategori</div>
                <div className="qrow__thumb">Görsel</div>
                <div className="qrow__product">Ürün</div>
                <div className="qrow__qty">Adet</div>
                <div className="qrow__price">B.Fiyat</div>
                <div className="qrow__total">Tutar</div>
                <div className="qrow__remove" />
              </div>

              {rows.map((row) => (
                <QuoteRow
                  key={row.uid}
                  row={row}
                  categoryOptions={categoryOptions}
                  products={productsForCategory(row.categorySlug)}
                  product={
                    row.productId != null
                      ? productById.get(row.productId)
                      : null
                  }
                  onCategoryChange={handleCategoryChange}
                  onProductChange={handleProductChange}
                  onQtyChange={handleQtyChange}
                  onRemove={handleRemove}
                />
              ))}
            </div>

            <button type="button" className="qbp__add" onClick={addRow}>
              + Ürün Ekle
            </button>
          </section>

          <div className="qbp__side">
            <QuoteSummary totalsByCurrency={totalsByCurrency} />
            <QuoteWarning />

            <button
              type="button"
              className="qbp__add-to-cart"
              onClick={handleAddToCart}
            >
              🛒 Sepete Taşı
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
