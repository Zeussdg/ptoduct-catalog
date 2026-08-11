import { useState } from "react";
import { useCart } from "../context/CartContext";
import { formatPrice } from "../utils/format";
import "./CartDrawer.css";

const EMPTY_CONTACT = { firma: "", yetkili: "", telefon: "", eposta: "" };

export default function CartDrawer() {
  const { items, isOpen, closeCart, setQty, removeItem, clearCart, totalsByCurrency } =
    useCart();

  const [quoteMode, setQuoteMode] = useState(false);
  const [contact, setContact] = useState(EMPTY_CONTACT);
  const [margin, setMargin] = useState("");
  const [generating, setGenerating] = useState(false);

  const marginPct = Number(margin) || 0;
  const factor = 1 + marginPct / 100;
  const currencies = Object.keys(totalsByCurrency);

  const updateContact = (key, value) =>
    setContact((prev) => ({ ...prev, [key]: value }));

  const handlePdf = async () => {
    setGenerating(true);
    try {
      const { generateQuotePdf } = await import("../utils/quotePdf");
      await generateQuotePdf({ items, totalsByCurrency, contact, margin: marginPct });
    } catch (err) {
      console.error("PDF oluşturulamadı:", err);
      alert("PDF oluşturulurken bir hata oluştu. Lütfen tekrar deneyin.");
    } finally {
      setGenerating(false);
    }
  };

  // Sepet kapanınca teklif modunu da sıfırla (bir sonraki açılış temiz başlasın).
  const handleClose = () => {
    setQuoteMode(false);
    closeCart();
  };

  return (
    <>
      <div
        className={"cdrw__overlay" + (isOpen ? " cdrw__overlay--visible" : "")}
        onClick={handleClose}
        aria-hidden="true"
      />
      <aside
        className={
          "cdrw" +
          (isOpen ? " cdrw--open" : "") +
          (quoteMode ? " cdrw--quote" : "")
        }
        aria-label="Sepet"
      >
        <div className="cdrw__head">
          <h2>{quoteMode ? "Teklif Oluştur" : "Sepet"}</h2>
          <button type="button" className="cdrw__close" onClick={handleClose} aria-label="Kapat">
            ×
          </button>
        </div>

        {items.length === 0 ? (
          <div className="cdrw__empty">
            <p>Sepetiniz boş.</p>
            <span>Ürün listesinden "Sepete Ekle" ile ürün ekleyebilirsiniz.</span>
          </div>
        ) : (
          <div className="cdrw__body">
            {/* ---- Sol panel: teklif formu (yalnızca teklif modunda) ---- */}
            {quoteMode && (
              <div className="cdrw__quote">
                <button
                  type="button"
                  className="cdrw__back"
                  onClick={() => setQuoteMode(false)}
                >
                  ← Sepete dön
                </button>

                <div className="cdrw__form">
                  <label className="cdrw__field">
                    <span>Firma</span>
                    <input
                      type="text"
                      value={contact.firma}
                      onChange={(e) => updateContact("firma", e.target.value)}
                      placeholder="Müşteri firma adı"
                    />
                  </label>
                  <label className="cdrw__field">
                    <span>Yetkili</span>
                    <input
                      type="text"
                      value={contact.yetkili}
                      onChange={(e) => updateContact("yetkili", e.target.value)}
                      placeholder="İlgili kişi"
                    />
                  </label>
                  <label className="cdrw__field">
                    <span>Telefon</span>
                    <input
                      type="tel"
                      value={contact.telefon}
                      onChange={(e) => updateContact("telefon", e.target.value)}
                      placeholder="+90 ..."
                    />
                  </label>
                  <label className="cdrw__field">
                    <span>E-posta</span>
                    <input
                      type="email"
                      value={contact.eposta}
                      onChange={(e) => updateContact("eposta", e.target.value)}
                      placeholder="ornek@firma.com"
                    />
                  </label>
                </div>

                <label className="cdrw__field cdrw__margin">
                  <span>Kâr Marjı (%)</span>
                  <input
                    type="number"
                    min="0"
                    step="1"
                    value={margin}
                    onChange={(e) => setMargin(e.target.value)}
                    placeholder="0"
                  />
                </label>

                <div className="cdrw__summary">
                  {currencies.length === 0 && (
                    <p className="cdrw__summary-empty">
                      Fiyatlandırılabilir ürün bulunmuyor.
                    </p>
                  )}
                  {currencies.map((cur) => {
                    const before = totalsByCurrency[cur];
                    const marginAmt = before * (marginPct / 100);
                    const after = before * factor;
                    return (
                      <div key={cur} className="cdrw__summary-group">
                        <div className="cdrw__summary-row">
                          <span>Ara Toplam ({cur})</span>
                          <span className="mono">{formatPrice(before, cur)}</span>
                        </div>
                        <div className="cdrw__summary-row cdrw__summary-row--muted">
                          <span>Kâr Marjı (%{marginPct})</span>
                          <span className="mono">+{formatPrice(marginAmt, cur)}</span>
                        </div>
                        <div className="cdrw__summary-row cdrw__summary-row--total">
                          <span>Teklif Toplamı ({cur})</span>
                          <span className="mono">{formatPrice(after, cur)}</span>
                        </div>
                      </div>
                    );
                  })}
                </div>

                <button
                  type="button"
                  className="cdrw__pdf"
                  onClick={handlePdf}
                  disabled={generating || currencies.length === 0}
                >
                  {generating ? "PDF hazırlanıyor…" : "Teklifi PDF'e geçir"}
                </button>
              </div>
            )}

            {/* ---- Sağ panel: sepet kalemleri ---- */}
            <div className="cdrw__cart">
              <ul className="cdrw__list">
                {items.map(({ product, qty }) => (
                  <li key={product.id} className="cdrw__item">
                    <div className="cdrw__item-info">
                      <span className="cdrw__item-brand">{product.brand}</span>
                      <span className="cdrw__item-name">{product.name}</span>
                      <span className="cdrw__item-code mono">{product.stockCode}</span>
                    </div>

                    <div className="cdrw__item-controls">
                      <div className="cdrw__qty">
                        <button
                          type="button"
                          onClick={() => setQty(product.id, qty - 1)}
                          aria-label="Azalt"
                        >
                          −
                        </button>
                        <span className="mono">{qty}</span>
                        <button
                          type="button"
                          onClick={() => setQty(product.id, qty + 1)}
                          aria-label="Artır"
                        >
                          +
                        </button>
                      </div>
                      <span className="cdrw__item-price mono">
                        {product.price != null
                          ? formatPrice(product.price * qty, product.currency)
                          : "—"}
                      </span>
                      <button
                        type="button"
                        className="cdrw__remove"
                        onClick={() => removeItem(product.id)}
                        aria-label="Kaldır"
                      >
                        Kaldır
                      </button>
                    </div>
                  </li>
                ))}
              </ul>

              <div className="cdrw__footer">
                <div className="cdrw__totals">
                  {currencies.map((cur) => (
                    <div key={cur} className="cdrw__total-row">
                      <span>Ara Toplam ({cur})</span>
                      <span className="mono">{formatPrice(totalsByCurrency[cur], cur)}</span>
                    </div>
                  ))}
                </div>

                {!quoteMode && (
                  <button
                    type="button"
                    className="cdrw__quote-btn"
                    onClick={() => setQuoteMode(true)}
                  >
                    Teklif Oluştur
                  </button>
                )}

                <button type="button" className="cdrw__clear" onClick={clearCart}>
                  Sepeti Temizle
                </button>
                <p className="cdrw__note">
                  Bu bir teklif listesidir; ödeme veya satın alma işlemi içermez.
                </p>
              </div>
            </div>
          </div>
        )}
      </aside>
    </>
  );
}
