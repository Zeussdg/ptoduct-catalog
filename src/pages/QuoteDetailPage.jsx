import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { quotesApi } from "../services/quotesApi";
import { formatPrice } from "../utils/format";
import "./customerPages.css";

const STATUS_LABELS = {
  PENDING: "Beklemede",
  REVIEWING: "İnceleniyor",
  OFFERED: "Teklif Verildi",
  ACCEPTED: "Kabul Edildi",
  REJECTED: "Reddedildi",
  CANCELLED: "İptal Edildi",
};

export default function QuoteDetailPage() {
  const { id } = useParams();
  const [quote, setQuote] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    quotesApi
      .getById(id)
      .then(({ quote }) => setQuote(quote))
      .catch(() => setError("Teklif bulunamadı"))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <div className="custpg">Yükleniyor...</div>;
  if (error || !quote) return <div className="custpg">{error || "Teklif bulunamadı"}</div>;

  return (
    <div className="custpg">
      <h1 className="custpg__title">
        Teklif #{quote.id} <span className="custpg__badge">{STATUS_LABELS[quote.status] ?? quote.status}</span>
      </h1>

      <div className="custpg__card">
        <table className="custpg__table">
          <thead>
            <tr>
              <th>Ürün</th>
              <th>Ürün Kodu</th>
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

      {quote.adminNote && (
        <div className="custpg__card">
          <strong>Admin Notu:</strong>
          <p>{quote.adminNote}</p>
        </div>
      )}
    </div>
  );
}
