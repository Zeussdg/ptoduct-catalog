import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { adminApi } from "../../services/adminApi";
import "./adminPages.css";

export default function CustomersAdminPage() {
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    adminApi.customers
      .list({ page: 1 })
      .then((res) => setCustomers(res.items))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <h1 className="adp__title">Müşteriler</h1>
      <p className="adp__subtitle">CUSTOMER rolündeki tüm kullanıcılar</p>

      <div className="adp__card">
        {loading ? (
          <div className="adp__empty">Yükleniyor...</div>
        ) : customers.length === 0 ? (
          <div className="adp__empty">Müşteri bulunamadı.</div>
        ) : (
          <table className="adp__table">
            <thead>
              <tr>
                <th>Firma</th>
                <th>Ad Soyad</th>
                <th>E-posta</th>
                <th>Durum</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {customers.map((c) => (
                <tr key={c.id}>
                  <td>{c.companyName ?? "—"}</td>
                  <td>
                    {c.name} {c.surname}
                  </td>
                  <td>{c.email}</td>
                  <td>
                    <span className={`adp__badge ${c.status === "ACTIVE" ? "" : "adp__badge--muted"}`}>
                      {c.status}
                    </span>
                  </td>
                  <td>
                    <Link className="adp__btn adp__btn--sm adp__btn--ghost" to={`/admin/customers/${c.id}`}>
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
