import { useState } from "react";
import { Link, useParams, useNavigate } from "react-router-dom";
import { products } from "../data/products";
import { useCart } from "../context/CartContext";
import { formatPrice } from "../utils/format";
import "./ProductDetailPage.css";

export default function ProductDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addItem } = useCart();
  const [qty, setQty] = useState(1);

  const product = products.find((p) => String(p.id) === id);

  if (!product) {
    return (
      <div className="pdetail__notfound">
        <p>Ürün bulunamadı.</p>
        <Link to="/">Ürün listesine dön</Link>
      </div>
    );
  }

  const related = products
    .filter((p) => p.categorySlug === product.categorySlug && p.id !== product.id)
    .slice(0, 4);

  return (
    <div className="pdetail">
      <div className="pdetail__inner">
        <button type="button" className="pdetail__back" onClick={() => navigate(-1)}>
          ← Geri
        </button>

        <div className="pdetail__breadcrumb">
          <Link to={`/?kategori=${product.mainCategorySlug}`}>{product.mainCategory}</Link>
          <span>/</span>
          <Link to={`/?kategori=${product.mainCategorySlug}&altkategori=${product.categorySlug}`}>
            {product.category}
          </Link>
        </div>

        <div className="pdetail__layout">
          <div className="pdetail__image">
            <div className="pdetail__image-placeholder">
              <span>{product.brand}</span>
            </div>
          </div>

          <div className="pdetail__info">
            <span className="pdetail__brand">{product.brand}</span>
            <h1 className="pdetail__name">{product.name}</h1>
            <span className="pdetail__code mono">Stok Kodu: {product.stockCode}</span>

            <div className="pdetail__price-row">
              <span className="pdetail__price">
                {formatPrice(product.price, product.currency)}
              </span>
            </div>

            <div className="pdetail__add-row">
              <div className="pdetail__qty">
                <button type="button" onClick={() => setQty((q) => Math.max(1, q - 1))}>
                  −
                </button>
                <span className="mono">{qty}</span>
                <button type="button" onClick={() => setQty((q) => q + 1)}>
                  +
                </button>
              </div>
              <button
                type="button"
                className="pdetail__add-btn"
                onClick={() => addItem(product, qty)}
              >
                Sepete Ekle
              </button>
            </div>

            <dl className="pdetail__specs">
              <div>
                <dt>Marka</dt>
                <dd>{product.brand}</dd>
              </div>
              <div>
                <dt>Stok Kodu</dt>
                <dd className="mono">{product.stockCode}</dd>
              </div>
              <div>
                <dt>Ana Kategori</dt>
                <dd>{product.mainCategory}</dd>
              </div>
              <div>
                <dt>Kategori</dt>
                <dd>{product.category}</dd>
              </div>
              <div>
                <dt>Para Birimi</dt>
                <dd>{product.currency}</dd>
              </div>
            </dl>

            <div className="pdetail__desc">
              <h2>Açıklama</h2>
              <p>{product.description}</p>
            </div>
          </div>
        </div>

        {related.length > 0 && (
          <div className="pdetail__related">
            <h2>Bu kategoriden diğer ürünler</h2>
            <ul>
              {related.map((r) => (
                <li key={r.id}>
                  <Link to={`/urun/${r.id}`}>
                    <span className="pdetail__related-brand">{r.brand}</span>
                    <span className="pdetail__related-name">{r.name}</span>
                    <span className="pdetail__related-price mono">
                      {formatPrice(r.price, r.currency)}
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}
