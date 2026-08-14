import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { productsApi } from "../../services/productsApi";
import { formatPrice } from "../../utils/format";
import "./adminPages.css";

export default function ProductsAdminPage() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);

  function load() {
    setLoading(true);
    productsApi
      .list({ sayfa: 1 })
      .then((res) => setProducts(res.items))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function handleDelete(id) {
    if (!window.confirm("Bu ürünü silmek istediğinize emin misiniz?")) return;
    try {
      await productsApi.remove(id);
      load();
    } catch (err) {
      alert(err.message || "Silme işlemi başarısız oldu");
    }
  }

  async function toggleActive(product) {
    await productsApi.update(product.id, { isActive: !product.isActive });
    load();
  }

  return (
    <div>
      <div className="adp__toolbar">
        <div>
          <h1 className="adp__title">Ürünler</h1>
          <p className="adp__subtitle">Ürün kataloğunu yönetin</p>
        </div>
        <Link className="adp__btn" to="/admin/products/new">
          + Yeni Ürün
        </Link>
      </div>

      <div className="adp__card">
        {loading ? (
          <div className="adp__empty">Yükleniyor...</div>
        ) : products.length === 0 ? (
          <div className="adp__empty">Ürün bulunamadı.</div>
        ) : (
          <table className="adp__table">
            <thead>
              <tr>
                <th>Ürün</th>
                <th>Kod</th>
                <th>Kategori</th>
                <th>Fiyat</th>
                <th>Durum</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {products.map((p) => (
                <tr key={p.id}>
                  <td>{p.name}</td>
                  <td className="mono">{p.stockCode}</td>
                  <td>{p.category?.name ?? "—"}</td>
                  <td>{formatPrice(p.discountPrice ?? p.price, p.currency)}</td>
                  <td>
                    <button
                      className={`adp__badge ${p.isActive ? "" : "adp__badge--muted"}`}
                      style={{ border: "none", cursor: "pointer" }}
                      onClick={() => toggleActive(p)}
                    >
                      {p.isActive ? "Aktif" : "Pasif"}
                    </button>
                  </td>
                  <td style={{ display: "flex", gap: 6 }}>
                    <Link className="adp__btn adp__btn--sm adp__btn--ghost" to={`/admin/products/${p.id}/edit`}>
                      Düzenle
                    </Link>
                    <button className="adp__btn adp__btn--sm adp__btn--danger" onClick={() => handleDelete(p.id)}>
                      Sil
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
