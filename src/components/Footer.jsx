import { Link } from "react-router-dom";
import { useCart } from "../context/CartContext";
import { categories } from "../data/categories";
import { site } from "../config/site";
import "./Footer.css";

// Footer'da öne çıkarılacak ana kategori sayısı; fazlası "Tüm Kategoriler"
// bağlantısıyla katalog sayfasına yönlendirilir.
const FEATURED_CATEGORY_COUNT = 6;

// Hızlı menü — Header ile aynı gerçek route'ları kullanır. "Sepet" bir
// sayfa değil, drawer olduğundan aşağıda ayrı bir buton olarak ele alınır.
const QUICK_LINKS = [
  { to: "/", label: "Ürünler" },
  { to: "/teklif-sihirbazi", label: "Teklif Sihirbazı" },
  { to: "/is-ortaklarimiz", label: "İş Ortaklarımız" },
];

// Sayfaları henüz mevcut olmayan yasal bağlantılar. Kırık route'a
// yönlendirmemek için tıklanabilir link değil, bilgilendirici etiket olarak
// gösterilir; ilgili sayfalar eklendiğinde `to` alanı verilebilir.
const LEGAL_LINKS = [
  { label: "Gizlilik Politikası" },
  { label: "KVKK" },
  { label: "Kullanım Koşulları" },
];

const SOCIAL_LINKS = [
  { key: "instagram", label: "Instagram", icon: InstagramIcon },
  { key: "linkedin", label: "LinkedIn", icon: LinkedInIcon },
  { key: "facebook", label: "Facebook", icon: FacebookIcon },
  { key: "youtube", label: "YouTube", icon: YouTubeIcon },
];

export default function Footer() {
  const { openCart } = useCart();

  const featuredCategories = categories.slice(0, FEATURED_CATEGORY_COUNT);
  const hasMoreCategories = categories.length > FEATURED_CATEGORY_COUNT;

  const activeSocial = SOCIAL_LINKS.filter(({ key }) => site.social[key]);
  const { address, phone, email, hours } = site.contact;
  const hasContact = address || phone || email || hours;

  const year = new Date().getFullYear();

  return (
    <footer className="ftr" aria-labelledby="ftr-heading">
      <h2 id="ftr-heading" className="ftr__sr-only">
        Site altbilgisi
      </h2>

      <div className="ftr__inner">
        {/* 1 — Firma */}
        <div className="ftr__col ftr__col--brand">
          <Link to="/" className="ftr__brand" aria-label={`${site.brand.name} ana sayfa`}>
            <img className="ftr__brand-logo" src={site.brand.logo} alt="" />
            <span className="ftr__brand-text">
              <span className="ftr__brand-name">{site.brand.name}</span>
              <span className="ftr__brand-sub">{site.brand.tagline}</span>
            </span>
          </Link>
          <p className="ftr__desc">{site.brand.description}</p>

          {activeSocial.length > 0 && (
            <ul className="ftr__social" aria-label="Sosyal medya">
              {activeSocial.map(({ key, label, icon: Icon }) => (
                <li key={key}>
                  <a
                    className="ftr__social-link"
                    href={site.social[key]}
                    target="_blank"
                    rel="noopener noreferrer"
                    aria-label={label}
                    title={label}
                  >
                    <Icon />
                  </a>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* 2 — Hızlı Menü */}
        <nav className="ftr__col" aria-label="Hızlı menü">
          <h3 className="ftr__title">Hızlı Menü</h3>
          <ul className="ftr__links">
            {QUICK_LINKS.map(({ to, label }) => (
              <li key={to}>
                <Link to={to} className="ftr__link">
                  {label}
                </Link>
              </li>
            ))}
            <li>
              <button type="button" className="ftr__link ftr__link--btn" onClick={openCart}>
                Sepet
              </button>
            </li>
          </ul>
        </nav>

        {/* 3 — Ürün Kategorileri */}
        <nav className="ftr__col" aria-label="Ürün kategorileri">
          <h3 className="ftr__title">Ürün Kategorileri</h3>
          <ul className="ftr__links">
            {featuredCategories.map((cat) => (
              <li key={cat.slug}>
                <Link to={`/?kategori=${cat.slug}`} className="ftr__link">
                  {cat.name}
                </Link>
              </li>
            ))}
            {hasMoreCategories && (
              <li>
                <Link to="/" className="ftr__link ftr__link--muted">
                  Tüm Kategoriler
                </Link>
              </li>
            )}
          </ul>
        </nav>

        {/* 4 — İletişim */}
        <div className="ftr__col">
          <h3 className="ftr__title">İletişim</h3>
          {hasContact ? (
            <ul className="ftr__contact">
              {address && (
                <li className="ftr__contact-item">
                  <PinIcon />
                  <span>{address}</span>
                </li>
              )}
              {phone && (
                <li className="ftr__contact-item">
                  <PhoneIcon />
                  <a className="ftr__link" href={`tel:${phone.replace(/\s+/g, "")}`}>
                    {phone}
                  </a>
                </li>
              )}
              {email && (
                <li className="ftr__contact-item">
                  <MailIcon />
                  <a className="ftr__link" href={`mailto:${email}`}>
                    {email}
                  </a>
                </li>
              )}
              {hours && (
                <li className="ftr__contact-item">
                  <ClockIcon />
                  <span>{hours}</span>
                </li>
              )}
            </ul>
          ) : (
            <p className="ftr__desc">
              Fiyat ve teklif talepleriniz için ürünlerinizi sepete ekleyip{" "}
              <Link to="/teklif-sihirbazi" className="ftr__link ftr__link--inline">
                Teklif Sihirbazı
              </Link>{" "}
              üzerinden bize iletebilirsiniz.
            </p>
          )}
        </div>
      </div>

      <div className="ftr__bottom">
        <div className="ftr__bottom-inner">
          <p className="ftr__copy">
            © {year} {site.brand.name} {site.brand.tagline}. Tüm hakları saklıdır.
          </p>
          <ul className="ftr__legal" aria-label="Yasal bilgiler">
            {LEGAL_LINKS.map(({ label }) => (
              <li key={label}>
                <span className="ftr__legal-link" title="Yakında">
                  {label}
                </span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </footer>
  );
}

/* ---- İkonlar — Header ile aynı 24px / 1.7 stroke çizgi stili ---- */

function iconProps() {
  return {
    width: 18,
    height: 18,
    viewBox: "0 0 24 24",
    fill: "none",
    "aria-hidden": true,
  };
}

function PinIcon() {
  return (
    <svg {...iconProps()}>
      <path
        d="M12 21s7-5.5 7-11a7 7 0 1 0-14 0c0 5.5 7 11 7 11Z"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="12" cy="10" r="2.4" stroke="currentColor" strokeWidth="1.7" />
    </svg>
  );
}

function PhoneIcon() {
  return (
    <svg {...iconProps()}>
      <path
        d="M4 5c0-.6.4-1 1-1h2.3c.5 0 .9.3 1 .8l.8 3c.1.4 0 .8-.3 1.1L7.4 10.5a12 12 0 0 0 6.1 6.1l1.6-1.4c.3-.3.7-.4 1.1-.3l3 .8c.5.1.8.5.8 1V19c0 .6-.4 1-1 1A16 16 0 0 1 4 5Z"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function MailIcon() {
  return (
    <svg {...iconProps()}>
      <rect x="3" y="5" width="18" height="14" rx="2" stroke="currentColor" strokeWidth="1.7" />
      <path
        d="m4 7 8 6 8-6"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function ClockIcon() {
  return (
    <svg {...iconProps()}>
      <circle cx="12" cy="12" r="8" stroke="currentColor" strokeWidth="1.7" />
      <path
        d="M12 8v4l2.5 2"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function InstagramIcon() {
  return (
    <svg {...iconProps()}>
      <rect x="3.5" y="3.5" width="17" height="17" rx="4.5" stroke="currentColor" strokeWidth="1.7" />
      <circle cx="12" cy="12" r="3.6" stroke="currentColor" strokeWidth="1.7" />
      <circle cx="16.6" cy="7.4" r="1" fill="currentColor" />
    </svg>
  );
}

function LinkedInIcon() {
  return (
    <svg {...iconProps()}>
      <rect x="3.5" y="3.5" width="17" height="17" rx="3" stroke="currentColor" strokeWidth="1.7" />
      <path
        d="M8 10.5V17M8 7.6v.1M11.7 17v-3.5c0-1.3.9-2.2 2.1-2.2s2 .9 2 2.2V17"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function FacebookIcon() {
  return (
    <svg {...iconProps()}>
      <path
        d="M14.5 8.5h1.8M14.5 8.5c0-1.4 0-2.5 2-2.5M14.5 8.5V21M14.5 12.5h-3m3 0h2.3"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function YouTubeIcon() {
  return (
    <svg {...iconProps()}>
      <rect x="3" y="6" width="18" height="12" rx="3.5" stroke="currentColor" strokeWidth="1.7" />
      <path d="m10.5 9.5 4 2.5-4 2.5v-5Z" fill="currentColor" />
    </svg>
  );
}
