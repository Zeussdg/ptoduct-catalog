import { Link, NavLink } from "react-router-dom";
import { useCart } from "../context/CartContext";
import "./Header.css";

export default function Header() {
  const { totalCount, openCart } = useCart();

  return (
    <header className="hdr">
      <div className="hdr__inner">
        <Link to="/" className="hdr__brand">
        <div className="hdr__brand-logo">
         <img  src="\public\logo.jpeg" alt="logo" />
          </div>
          <span className="hdr__brand-text">
            <span className="hdr__brand-name">B2B</span>
            <span className="hdr__brand-sub">Ürün Kataloğu</span>
          </span>
        </Link>

        <nav className="hdr__nav" aria-label="Ana menü">
          <NavLink
            to="/"
            end
            className={({ isActive }) =>
              `hdr__nav-link${isActive ? " hdr__nav-link--active" : ""}`
            }
          >
            Ürünler
          </NavLink>
          <NavLink
            to="/teklif-sihirbazi"
            className={({ isActive }) =>
              `hdr__nav-link${isActive ? " hdr__nav-link--active" : ""}`
            }
          >
            Teklif Sihirbazı
          </NavLink>
          <NavLink
            to="/is-ortaklarimiz"
            className={({ isActive }) =>
              `hdr__nav-link${isActive ? " hdr__nav-link--active" : ""}`
            }
          >
            İş Ortaklarımız
          </NavLink>
        </nav>

        <button
          type="button"
          className="hdr__cart"
          onClick={openCart}
          aria-label={`Sepeti aç, ${totalCount} ürün`}
        >
          <svg width="19" height="19" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path
              d="M3 4h2l1.6 10.6a2 2 0 0 0 2 1.7h7.4a2 2 0 0 0 2-1.6L20 8H6"
              stroke="currentColor"
              strokeWidth="1.7"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            <circle cx="9.5" cy="20" r="1.4" fill="currentColor" />
            <circle cx="17" cy="20" r="1.4" fill="currentColor" />
          </svg>
          <span>Sepet</span>
          {totalCount > 0 && <span className="hdr__cart-badge">{totalCount}</span>}
        </button>
      </div>
    </header>
  );
}
