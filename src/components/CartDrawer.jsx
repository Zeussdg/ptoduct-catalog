import { useState } from "react";
import { useCart } from "../context/CartContext";
import { formatPrice } from "../utils/format";
import "./CartDrawer.css";

const EMPTY_CONTACT = { firma: "", yetkili: "", telefon: "", eposta: "" };

export default function CartDrawer() {
  const {
    items,
    isOpen,
    closeCart,
    setQty,
    removeItem,
    clearCart,
    totalsByCurrency,
  } = useCart();

  const [quoteMode, setQuoteMode] = useState(false);
  const [contact, setContact] = useState({
    firma: "",
    yetkili: "",
    telefon: "",
    eposta: "",
  });
  const [seller, setSeller] = useState({
    firma: "",
    yetkili: "",
    telefon: "",
    eposta: "",
  });
  const [margin, setMargin] = useState("");
  const [generating, setGenerating] = useState(false);

  const marginPct = Number(margin) || 0;
  const factor = 1 + marginPct / 100;
  const currencies = Object.keys(totalsByCurrency);

  const updateContact = (key, value) =>
    setContact((prev) => ({ ...prev, [key]: value }));

  const updateSeller = (key, value) =>
    setSeller((prev) => ({ ...prev, [key]: value }));

  const handlePdf = async () => {
    setGenerating(true);
    try {
      const { generateQuotePdf } = await import("../utils/quotePdf");
      await generateQuotePdf({
        items,
        totalsByCurrency,
        seller,
        contact,
        margin: marginPct,
      });
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
          <button
            type="button"
            className="cdrw__close"
            onClick={handleClose}
            aria-label="Kapat"
          >
            ×
          </button>
        </div>

        {items.length === 0 ? (
          <div className="cdrw__empty">
            <p>Sepetiniz boş.</p>
            <span>
              Ürün listesinden "Sepete Ekle" ile ürün ekleyebilirsiniz.
            </span>
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
                  {/* TEKLİFİ VEREN */}
                  <div className="cdrw__form-section">
                    <h3>Teklifi Veren</h3>

                    <label className="cdrw__field">
                      <span>Firma</span>
                      <input
                        type="text"
                        value={seller.firma}
                        onChange={(e) => updateSeller("firma", e.target.value)}
                        placeholder="Firma adı"
                      />
                    </label>

                    <label className="cdrw__field">
                      <span>Yetkili</span>
                      <input
                        type="text"
                        value={seller.yetkili}
                        onChange={(e) =>
                          updateSeller("yetkili", e.target.value)
                        }
                        placeholder="Yetkili kişi"
                      />
                    </label>

                    <label className="cdrw__field">
                      <span>Telefon</span>
                      <input
                        type="tel"
                        value={seller.telefon}
                        onChange={(e) =>
                          updateSeller("telefon", e.target.value)
                        }
                        placeholder="+90 ..."
                      />
                    </label>

                    <label className="cdrw__field">
                      <span>E-posta</span>
                      <input
                        type="email"
                        value={seller.eposta}
                        onChange={(e) => updateSeller("eposta", e.target.value)}
                        placeholder="satis@firma.com"
                      />
                    </label>
                  </div>

                  {/* TEKLİFİ ALAN */}
                  <div className="cdrw__form-section">
                    <h3>Teklifi Alan</h3>

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
                        onChange={(e) =>
                          updateContact("yetkili", e.target.value)
                        }
                        placeholder="İlgili kişi"
                      />
                    </label>
                  </div>
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
                    const before = Number(totalsByCurrency[cur]) || 0;

                    // Kâr marjı
                    const marginAmt = before * (marginPct / 100);

                    // Kâr eklenmiş toplam
                    const after = before + marginAmt;

                    // KDV, kâr eklenmiş tutar üzerinden
                    const vatAmt = after * 0.2;

                    // KDV dahil genel toplam
                    const grandTotal = after + vatAmt;

                    return (
                      <div key={cur} className="cdrw__summary-group">
                        <div className="cdrw__summary-row">
                          <span>Ara Toplam ({cur})</span>
                          <span className="mono">
                            {formatPrice(before, cur)}
                          </span>
                        </div>
                        <div className="cdrw__summary-row cdrw__summary-row--muted">
                          <span>Kâr Marjı (%{marginPct})</span>
                          <span className="mono">
                            +{formatPrice(marginAmt, cur)}
                          </span>
                        </div>
                        <div className="cdrw__summary-row cdrw__summary-row--muted">
                          <span>KDV (%20)</span>
                          <span className="mono">
                            +{formatPrice(vatAmt, cur)}
                          </span>
                        </div>
                        <div className="cdrw__summary-row cdrw__summary-row--total">
                          <span>Teklif Toplamı ({cur})</span>
                          <span className="mono">
                            {formatPrice(grandTotal, cur)}
                          </span>
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
                      <span className="cdrw__item-code mono">
                        {product.stockCode}
                      </span>
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
                  {/* Ara Toplam */}
                  {currencies.map((cur) => (
                    <div key={`subtotal-${cur}`} className="cdrw__total-row">
                      <span>Ara Toplam ({cur})</span>
                      <span className="mono">
                        {formatPrice(totalsByCurrency[cur], cur)}
                      </span>
                    </div>
                  ))}

                  {/* KDV %20 */}
                  {currencies.map((cur) => {
                    const subtotal = totalsByCurrency[cur];
                    const vat = subtotal * 0.2;

                    return (
                      <div key={`vat-${cur}`} className="cdrw__total-row">
                        <span>KDV (%20) ({cur})</span>
                        <span className="mono">{formatPrice(vat, cur)}</span>
                      </div>
                    );
                  })}

                  {/* Genel Toplam */}
                  {currencies.map((cur) => {
                    const subtotal = totalsByCurrency[cur];
                    const vat = subtotal * 0.2;
                    const grandTotal = subtotal + vat;

                    return (
                      <div
                        key={`grand-total-${cur}`}
                        className="cdrw__total-row cdrw__total-row--grand"
                      >
                        <span>Genel Toplam ({cur})</span>
                        <span className="mono">
                          {formatPrice(grandTotal, cur)}
                        </span>
                      </div>
                    );
                  })}
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

                <button
                  type="button"
                  className="cdrw__clear"
                  onClick={clearCart}
                >
                  Sepeti Temizle
                </button>
                <p className="cdrw__note">
                  Bu bir teklif listesidir; ödeme veya satın alma işlemi
                  içermez.
                </p>
              </div>
            </div>
          </div>
        )}
      </aside>
    </>
  );
}
