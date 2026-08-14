import { useEffect, useState } from "react";
import { useAuth } from "../../hooks/useAuth";
import { adminApi } from "../../services/adminApi";
import "./adminPages.css";

const ROLES = ["CUSTOMER", "ADMIN", "SUPER_ADMIN"];
const STATUSES = ["ACTIVE", "INACTIVE", "SUSPENDED"];
const EMPTY_FORM = { email: "", username: "", password: "", role: "ADMIN", name: "", surname: "" };

export default function UsersAdminPage() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState([]);
  const [roleFilter, setRoleFilter] = useState("");
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  function load() {
    adminApi.users.list({ role: roleFilter }).then((res) => setUsers(res.items));
  }

  useEffect(load, [roleFilter]);

  async function handleCreate(e) {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      await adminApi.users.create(form);
      setForm(EMPTY_FORM);
      load();
    } catch (err) {
      setError(err.message || "Kullanıcı oluşturulamadı");
    } finally {
      setSaving(false);
    }
  }

  async function handleRoleChange(u, role) {
    await adminApi.users.updateRole(u.id, role);
    load();
  }

  async function handleStatusChange(u, status) {
    await adminApi.users.updateStatus(u.id, status);
    load();
  }

  async function handleDelete(u) {
    if (!window.confirm(`"${u.email}" kullanıcısını silmek istediğinize emin misiniz?`)) return;
    await adminApi.users.remove(u.id);
    load();
  }

  return (
    <div>
      <h1 className="adp__title">Kullanıcı Yönetimi</h1>
      <p className="adp__subtitle">Yalnızca SUPER_ADMIN erişebilir — roller ve admin hesapları burada yönetilir</p>

      <form className="adp__card" onSubmit={handleCreate}>
        <h2 style={{ fontSize: 14, margin: "0 0 12px" }}>Yeni Kullanıcı / Admin Oluştur</h2>
        <div className="adp__form-grid">
          <label className="adp__field">
            <span>E-posta</span>
            <input
              type="email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              required
            />
          </label>
          <label className="adp__field">
            <span>Kullanıcı Adı</span>
            <input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
          </label>
          <label className="adp__field">
            <span>Şifre</span>
            <input
              type="password"
              minLength={8}
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              required
            />
          </label>
          <label className="adp__field">
            <span>Rol</span>
            <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
              {ROLES.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>
          </label>
          <label className="adp__field">
            <span>Ad</span>
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </label>
          <label className="adp__field">
            <span>Soyad</span>
            <input value={form.surname} onChange={(e) => setForm({ ...form, surname: e.target.value })} />
          </label>
        </div>
        {error && <p style={{ color: "var(--danger-600)" }}>{error}</p>}
        <button className="adp__btn" type="submit" disabled={saving}>
          {saving ? "Oluşturuluyor..." : "Oluştur"}
        </button>
      </form>

      <div className="adp__toolbar">
        <h2 style={{ fontSize: 14, margin: 0 }}>Tüm Kullanıcılar</h2>
        <select className="adp__select-inline" value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)}>
          <option value="">Tüm roller</option>
          {ROLES.map((r) => (
            <option key={r} value={r}>
              {r}
            </option>
          ))}
        </select>
      </div>

      <div className="adp__card">
        <table className="adp__table">
          <thead>
            <tr>
              <th>E-posta</th>
              <th>Ad Soyad</th>
              <th>Rol</th>
              <th>Durum</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => {
              const isSelf = u.id === currentUser.id;
              return (
                <tr key={u.id}>
                  <td>{u.email}</td>
                  <td>
                    {u.name} {u.surname}
                  </td>
                  <td>
                    <select
                      className="adp__select-inline"
                      value={u.role}
                      disabled={isSelf}
                      onChange={(e) => handleRoleChange(u, e.target.value)}
                    >
                      {ROLES.map((r) => (
                        <option key={r} value={r}>
                          {r}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>
                    <select
                      className="adp__select-inline"
                      value={u.status}
                      disabled={isSelf}
                      onChange={(e) => handleStatusChange(u, e.target.value)}
                    >
                      {STATUSES.map((s) => (
                        <option key={s} value={s}>
                          {s}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>
                    {!isSelf && (
                      <button className="adp__btn adp__btn--sm adp__btn--danger" onClick={() => handleDelete(u)}>
                        Sil
                      </button>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
