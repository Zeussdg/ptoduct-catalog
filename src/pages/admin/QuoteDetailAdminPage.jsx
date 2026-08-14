import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { adminApi } from "../../services/adminApi";
import { formatPrice } from "../../utils/format";
import "./adminPages.css";

const STATUSES = ["PENDING", "REVIEWING", "OFFERED", "ACCEPTED", "REJECTED", "CANCELLED"];

export default function QuoteDetailAdminPage() {
  const { id } = useParams();
  const [quote, setQuote] = useState(null);
  const [status, setStatus] = useState("");
  const [adminNote, setAdminNote] = useState("");
  const [saving, setSaving] = useState(false);

  function load() {
    adminApi.quotes.getById(id).then(({ quote }) => {
      setQuote(quote);
      setStatus(quote.status);
      setAdminNote(quote.adminNote ?? "");
    });
  }

  useEffect(load, [id]);

  async function handleSave() {
    setSaving(true);
    try {
      const { quote: updated } = await adminApi.quotes.updateStatus(id, status, adminNote);
      setQuote(updated);
    } finally {
      setSaving(false);
    }
  }

  if (!quote) return <div className="adp__card">Yükleniyor...</div>;

  return (
    <div>
      <h1 className="adp__title">Teklif #{quote.id}</h1>

      <div className="adp__card">
        <h2 style={{ fontSize: 14, margin: "0 0 8px" }}>Müşteri Bilgileri</h2>
        <p style={{ margin: 0 }}>{quote.user?.companyName}</p>
        <p style={{ margin: 0, color: "var(--ink-500)" }}>
          {quote.user?.name} {quote.user?.surname} · {quote.user?.email} · {quote.user?.phone}
        </p>
      </div>

      <div className="adp__card">
        <table className="adp__table">
          <thead>
            <tr>
              <th>Ürün</th>
              <th>Kod</th>
              <th>Adet</th>
              <th>Birim Fiyat</th>
              <th>Toplam</th>
            </tr>
          </thead>
          <tbody>
            {quote.items.map((item) => (
              <tr key={item.id}>
                <td>{item.productName}</td>
                <td className="mono">{item.productCode}</td>
                <td>{item.qty}</td>
                <td>{formatPrice(item.unitPrice, item.currency)}</td>
                <td>{formatPrice(item.totalPrice, item.currency)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="adp__card">
        <label className="adp__field">
          <span>Durum</span>
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </label>
        <label className="adp__field">
          <span>Admin Notu</span>
          <textarea rows={3} value={adminNote} onChange={(e) => setAdminNote(e.target.value)} />
        </label>
        <button className="adp__btn" onClick={handleSave} disabled={saving}>
          {saving ? "Kaydediliyor..." : "Kaydet"}
        </button>
      </div>
    </div>
  );
}
