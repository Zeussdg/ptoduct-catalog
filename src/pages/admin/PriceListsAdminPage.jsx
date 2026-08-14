import { useEffect, useState } from "react";
import { adminApi } from "../../services/adminApi";
import "./adminPages.css";

export default function PriceListsAdminPage() {
  const [priceLists, setPriceLists] = useState([]);
  const [name, setName] = useState("");
  const [saving, setSaving] = useState(false);

  function load() {
    adminApi.priceLists.list().then(({ priceLists }) => setPriceLists(priceLists));
  }

  useEffect(load, []);

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    try {
      await adminApi.priceLists.create({ name });
      setName("");
      load();
    } finally {
      setSaving(false);
    }
  }

  return (
    <div>
      <h1 className="adp__title">Fiyatlandırma</h1>
      <p className="adp__subtitle">
        Fiyat listeleri ve müşteriye özel fiyatlandırma altyapısı. Ürün bazlı standart fiyat/indirim için
        Ürünler sayfasını kullanın.
      </p>

      <form className="adp__card" onSubmit={handleSubmit}>
        <label className="adp__field">
          <span>Yeni Fiyat Listesi</span>
          <input value={name} onChange={(e) => setName(e.target.value)} required />
        </label>
        <button className="adp__btn" type="submit" disabled={saving}>
          {saving ? "Ekleniyor..." : "Ekle"}
        </button>
      </form>

      <div className="adp__card">
        {priceLists.length === 0 ? (
          <div className="adp__empty">Fiyat listesi yok.</div>
        ) : (
          <table className="adp__table">
            <thead>
              <tr>
                <th>Ad</th>
                <th>Durum</th>
              </tr>
            </thead>
            <tbody>
              {priceLists.map((pl) => (
                <tr key={pl.id}>
                  <td>{pl.name}</td>
                  <td>
                    <span className={`adp__badge ${pl.isActive ? "" : "adp__badge--muted"}`}>
                      {pl.isActive ? "Aktif" : "Pasif"}
                    </span>
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
