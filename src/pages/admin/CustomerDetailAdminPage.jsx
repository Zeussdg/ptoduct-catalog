import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { adminApi } from "../../services/adminApi";
import "./adminPages.css";

export default function CustomerDetailAdminPage() {
  const { id } = useParams();
  const [customer, setCustomer] = useState(null);

  useEffect(() => {
    adminApi.customers.getById(id).then(({ user }) => setCustomer(user));
  }, [id]);

  if (!customer) return <div className="adp__card">Yükleniyor...</div>;

  return (
    <div>
      <h1 className="adp__title">{customer.companyName ?? customer.email}</h1>

      <div className="adp__card">
        <p style={{ margin: 0 }}>
          {customer.name} {customer.surname} · {customer.email} · {customer.phone}
        </p>
      </div>

      <div className="adp__card">
        <h2 style={{ fontSize: 14, margin: "0 0 12px" }}>Teklif Geçmişi</h2>
        {customer.quotes?.length ? (
          <table className="adp__table">
            <thead>
              <tr>
                <th>Teklif No</th>
                <th>Durum</th>
                <th>Tarih</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {customer.quotes.map((q) => (
                <tr key={q.id}>
                  <td>#{q.id}</td>
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
        ) : (
          <div className="adp__empty">Teklif geçmişi yok.</div>
        )}
      </div>
    </div>
  );
}
