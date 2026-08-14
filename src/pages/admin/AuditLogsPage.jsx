import { useEffect, useState } from "react";
import { adminApi } from "../../services/adminApi";
import "./adminPages.css";

export default function AuditLogsPage() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    adminApi.auditLogs
      .list({ page: 1 })
      .then((res) => setLogs(res.logs))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <h1 className="adp__title">Audit Logs</h1>
      <p className="adp__subtitle">Admin ve Süper Admin işlem kayıtları</p>

      <div className="adp__card">
        {loading ? (
          <div className="adp__empty">Yükleniyor...</div>
        ) : logs.length === 0 ? (
          <div className="adp__empty">Kayıt yok.</div>
        ) : (
          <table className="adp__table">
            <thead>
              <tr>
                <th>Tarih</th>
                <th>Kullanıcı</th>
                <th>İşlem</th>
                <th>Varlık</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id}>
                  <td>{new Date(log.createdAt).toLocaleString("tr-TR")}</td>
                  <td>{log.actor?.email ?? "Sistem"}</td>
                  <td>
                    <span className="adp__badge">{log.action}</span>
                  </td>
                  <td className="mono">
                    {log.entityType} #{log.entityId}
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
