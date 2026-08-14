import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { productsApi } from "../../services/productsApi";
import { adminApi } from "../../services/adminApi";
import "./adminPages.css";

export default function DashboardPage() {
  const [kpis, setKpis] = useState(null);
  const [recentQuotes, setRecentQuotes] = useState([]);

  useEffect(() => {
    Promise.all([
      productsApi.list({ sayfa: 1 }),
      adminApi.customers.list({ page: 1 }),
      adminApi.quotes.list({ status: "PENDING", page: 1 }),
      adminApi.quotes.list({ page: 1 }),
    ]).then(([products, customers, pendingQuotes, latestQuotes]) => {
      setKpis({
        totalProducts: products.total,
        totalCustomers: customers.total,
        pendingQuotes: pendingQuotes.total,
      });
      setRecentQuotes(latestQuotes.items.slice(0, 5));
    });
  }, []);

  return (
    <div>
      <h1 className="adp__title">Dashboard</h1>
      <p className="adp__subtitle">Genel bakış</p>

      <div className="adp__kpis">
        <div className="adp__kpi">
          <div className="adp__kpi-value">{kpis?.totalProducts ?? "—"}</div>
          <div className="adp__kpi-label">Toplam Ürün</div>
        </div>
        <div className="adp__kpi">
          <div className="adp__kpi-value">{kpis?.totalCustomers ?? "—"}</div>
          <div className="adp__kpi-label">Toplam Müşteri</div>
        </div>
        <div className="adp__kpi">
          <div className="adp__kpi-value">{kpis?.pendingQuotes ?? "—"}</div>
          <div className="adp__kpi-label">Bekleyen Teklifler</div>
        </div>
      </div>

      <div className="adp__card">
        <h2 style={{ fontSize: 15, margin: "0 0 12px" }}>Son Teklifler</h2>
        {recentQuotes.length === 0 ? (
          <div className="adp__empty">Henüz teklif yok.</div>
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
              {recentQuotes.map((q) => (
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
