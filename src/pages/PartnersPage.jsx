import { partners } from "../data/partners";
import "./PartnersPage.css";

// Firma adından placeholder için baş harf(ler) üretir (logo yoksa).
function initials(name) {
  const words = name.replace(/[^\p{L}\s]/gu, "").trim().split(/\s+/);
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
  return (words[0][0] + words[1][0]).toUpperCase();
}

export default function PartnersPage() {
  return (
    <div className="partners">
      <div className="partners__inner">
        <header className="partners__head">
          <span className="partners__eyebrow">Çözüm Ortaklarımız</span>
          <h1 className="partners__title">İş Ortaklarımız</h1>
          <p className="partners__lead">
            Ağ, güvenlik, enerji ve altyapı alanlarında birlikte çalıştığımız
            markalar. Distribütörlüğünü ve çözüm ortaklığını yürüttüğümüz bu
            firmaların ürünlerini katalogumuzda bulabilirsiniz.
          </p>
        </header>

        <ul className="partners__grid">
          {partners.map((p) => (
            <li key={p.id}>
              <article className="pcard2" style={{ "--accent": p.accent }}>
                <div className="pcard2__top">
                  <div className="pcard2__logo">
                    {p.logo ? (
                      <img src={p.logo} alt={`${p.name} logosu`} />
                    ) : (
                      <span className="pcard2__logo-ph">{initials(p.name)}</span>
                    )}
                  </div>
                  <span className="pcard2__cat">{p.category}</span>
                </div>

                <h2 className="pcard2__name">{p.name}</h2>
                <p className="pcard2__desc">{p.description}</p>

                {p.website && (
                  <a
                    className="pcard2__link"
                    href={p.website}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    Web sitesi
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                      <path d="M7 17L17 7M9 7h8v8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  </a>
                )}
              </article>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
