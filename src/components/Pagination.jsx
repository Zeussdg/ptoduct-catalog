import "./Pagination.css";

// Görünecek sayfa numaralarını üretir: ilk, son, aktifin komşuları ve
// aradaki boşluklar için "…" (ellipsis).
function pageList(current, total) {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
  const pages = new Set([1, total, current, current - 1, current + 1]);
  const sorted = [...pages].filter((p) => p >= 1 && p <= total).sort((a, b) => a - b);
  const out = [];
  let prev = 0;
  for (const p of sorted) {
    if (p - prev > 1) out.push("…");
    out.push(p);
    prev = p;
  }
  return out;
}

export default function Pagination({ current, total, onChange }) {
  if (total <= 1) return null;

  const items = pageList(current, total);

  return (
    <nav className="pgn" aria-label="Sayfalama">
      <button
        type="button"
        className="pgn__nav"
        onClick={() => onChange(current - 1)}
        disabled={current === 1}
        aria-label="Önceki sayfa"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M15 6l-6 6 6 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
        <span>Önceki</span>
      </button>

      <ul className="pgn__list">
        {items.map((it, i) =>
          it === "…" ? (
            <li key={`gap-${i}`} className="pgn__gap" aria-hidden="true">
              …
            </li>
          ) : (
            <li key={it}>
              <button
                type="button"
                className={`pgn__page${it === current ? " pgn__page--active" : ""}`}
                onClick={() => onChange(it)}
                aria-current={it === current ? "page" : undefined}
                aria-label={`Sayfa ${it}`}
              >
                {it}
              </button>
            </li>
          )
        )}
      </ul>

      <button
        type="button"
        className="pgn__nav"
        onClick={() => onChange(current + 1)}
        disabled={current === total}
        aria-label="Sonraki sayfa"
      >
        <span>Sonraki</span>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M9 6l6 6-6 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>
    </nav>
  );
}
