import { useEffect, useMemo, useRef, useState } from "react";
import "./ProductCombo.css";

const TR_LOWER = (s) => (s || "").toLocaleLowerCase("tr-TR");

// Aranabilir ürün dropdown'u. Kategori seçilmeden disabled.
// options: seçilebilir ürün listesi, value: seçili productId, onChange(id).
export default function ProductCombo({ options, value, onChange, disabled }) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const rootRef = useRef(null);
  const inputRef = useRef(null);

  const selected = useMemo(
    () => options.find((p) => p.id === value) || null,
    [options, value],
  );

  const filtered = useMemo(() => {
    const q = TR_LOWER(query.trim());
    if (!q) return options;
    return options.filter(
      (p) =>
        TR_LOWER(p.name).includes(q) ||
        TR_LOWER(p.brand).includes(q) ||
        TR_LOWER(p.stockCode).includes(q),
    );
  }, [options, query]);

  // Dışarı tıklanınca kapat
  useEffect(() => {
    if (!open) return undefined;
    const onDoc = (e) => {
      if (rootRef.current && !rootRef.current.contains(e.target))
        setOpen(false);
    };
    document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, [open]);

  useEffect(() => {
    if (open && inputRef.current) inputRef.current.focus();
  }, [open]);

  const pick = (p) => {
    onChange(p.id);
    setOpen(false);
    setQuery("");
  };

  const label = selected
    ? `${selected.brand} ${selected.name}`
    : disabled
      ? "Lütfen önce kategori seçiniz"
      : "Ürün Seçiniz";

  return (
    <div className={"pcombo" + (open ? " pcombo--open" : "")} ref={rootRef}>
      <button
        type="button"
        className={
          "pcombo__trigger" + (selected ? "" : " pcombo__trigger--empty")
        }
        onClick={() => !disabled && setOpen((o) => !o)}
        disabled={disabled}
        aria-haspopup="listbox"
        aria-expanded={open}
      >
        <span className="pcombo__label">{label}</span>
        <span className="pcombo__caret" aria-hidden="true">
          ▾
        </span>
      </button>

      {open && (
        <div className="pcombo__panel">
          <input
            ref={inputRef}
            type="text"
            className="pcombo__search"
            placeholder="Ürün ara (ad, marka, stok kodu)…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <ul className="pcombo__list" role="listbox">
            {filtered.length === 0 && (
              <li className="pcombo__empty">Ürün bulunamadı.</li>
            )}
            {filtered.map((p) => (
              <li key={p.id}>
                <button
                  type="button"
                  className={
                    "pcombo__opt" +
                    (p.id === value ? " pcombo__opt--active" : "")
                  }
                  onClick={() => pick(p)}
                  role="option"
                  aria-selected={p.id === value}
                >
                  <span className="pcombo__opt-name">
                    <span className="pcombo__opt-brand">{p.brand}</span>{" "}
                    {p.name}
                  </span>
                  <span className="pcombo__opt-code mono">{p.stockCode}</span>
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
