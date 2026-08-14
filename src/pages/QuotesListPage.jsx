import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { quotesApi } from "../services/quotesApi";
import "./customerPages.css";

const STATUS_LABELS = {
  PENDING: "Beklemede",
  REVIEWING: "İnceleniyor",
  OFFERED: "Teklif Verildi",
  ACCEPTED: "Kabul Edildi",
  REJECTED: "Reddedildi",
  CANCELLED: "İptal Edildi",
};

export default function QuotesListPage() {
  const [quotes, setQuotes] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    quotesApi
      .listMine()
      .then(({ quotes }) => setQuotes(quotes))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="custpg">Yükleniyor...</div>;

  return (
    <div className="custpg">
      <h1 className="custpg__title">Tekliflerim</h1>
      <div className="custpg__card">
        {quotes.length === 0 ? (
          <div className="custpg__empty">Henüz teklif talebiniz yok.</div>
        ) : (
          <table className="custpg__table">
            <thead>
              <tr>
                <th>Teklif No</th>
                <th>Tarih</th>
                <th>Durum</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {quotes.map((q) => (
                <tr key={q.id}>
                  <td>#{q.id}</td>
                  <td>{new Date(q.createdAt).toLocaleDateString("tr-TR")}</td>
                  <td>
                    <span className="custpg__badge">{STATUS_LABELS[q.status] ?? q.status}</span>
                  </td>
                  <td>
                    <Link className="custpg__link" to={`/quotes/${q.id}`}>
                      Detay
                    </Link>
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
