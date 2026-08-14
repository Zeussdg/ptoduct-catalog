import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { adminApi } from "../../services/adminApi";
import "./adminPages.css";

const STATUSES = ["PENDING", "REVIEWING", "OFFERED", "ACCEPTED", "REJECTED", "CANCELLED"];

export default function QuotesAdminPage() {
  const [quotes, setQuotes] = useState([]);
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    adminApi.quotes
      .list({ status, page: 1 })
      .then((res) => setQuotes(res.items))
      .finally(() => setLoading(false));
  }, [status]);

  return (
    <div>
      <div className="adp__toolbar">
        <div>
          <h1 className="adp__title">Teklifler</h1>
          <p className="adp__subtitle">Tüm müşteri teklif taleplerini görüntüleyin</p>
        </div>
        <select className="adp__select-inline" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">Tüm durumlar</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>

      <div className="adp__card">
        {loading ? (
          <div className="adp__empty">Yükleniyor...</div>
        ) : quotes.length === 0 ? (
          <div className="adp__empty">Teklif bulunamadı.</div>
        ) : (
          <table className="adp__table">
            <thead>
              <tr>
                <th>Teklif No</th>
                <th>Müşteri</th>
                <th>Durum</th>
                <th>Tarih</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {quotes.map((q) => (
                <tr key={q.id}>
                  <td>#{q.id}</td>
                  <td>{q.user?.companyName ?? q.user?.email}</td>
                  <td>
                    <span className="adp__badge">{q.status}</span>
                  </td>
                  <td>{new Date(q.createdAt).toLocaleDateString("tr-TR")}</td>
                  <td>
                    <Link className="adp__btn adp__btn--sm adp__btn--ghost" to={`/admin/quotes/${q.id}`}>
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
