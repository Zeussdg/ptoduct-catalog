import { NavLink, Outlet, Link } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import "./AdminLayout.css";

const NAV_ITEMS = [
  { to: "/admin", label: "Dashboard", end: true },
  { to: "/admin/products", label: "Ürünler" },
  { to: "/admin/categories", label: "Kategoriler" },
  { to: "/admin/campaign-banners", label: "Kampanya Görselleri" },
  { to: "/admin/price-lists", label: "Fiyatlandırma" },
  { to: "/admin/quotes", label: "Teklifler" },
  { to: "/admin/customers", label: "Müşteriler" },
];

const SUPER_ADMIN_NAV_ITEMS = [
  { to: "/admin/users", label: "Kullanıcı Yönetimi" },
  { to: "/admin/audit-logs", label: "Audit Logs" },
];

export default function AdminLayout() {
  const { user, logout } = useAuth();
  const isSuperAdmin = user?.role === "SUPER_ADMIN";

  return (
    <div className="adm">
      <aside className="adm__sidebar">
        <Link to="/" className="adm__brand">
          Ürün Kataloğu
        </Link>
        <nav className="adm__nav">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `adm__navlink${isActive ? " adm__navlink--active" : ""}`}
            >
              {item.label}
            </NavLink>
          ))}
          {isSuperAdmin && (
            <>
              <div className="adm__navdivider">Süper Admin</div>
              {SUPER_ADMIN_NAV_ITEMS.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) => `adm__navlink${isActive ? " adm__navlink--active" : ""}`}
                >
                  {item.label}
                </NavLink>
              ))}
            </>
          )}
        </nav>
      </aside>
      <div className="adm__main">
        <header className="adm__topbar">
          <div className="adm__topbar-user">
            <span className="adm__topbar-name">
              {user?.name ?? user?.email} <span className="adm__topbar-role">({user?.role})</span>
            </span>
          </div>
          <button type="button" className="adm__logout" onClick={logout}>
            Çıkış Yap
          </button>
        </header>
        <main className="adm__content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
