import { formatPrice } from "../utils/format";
import { VAT_RATE } from "../config/pricing";
import "./QuoteCards.css";

// Para birimi bazında toplam kartı. totalsByCurrency: { USD: number, ... }
// KDV oranı merkezi config'ten (VAT_RATE) gelir. Farklı para birimleri
// asla toplanmaz — her biri ayrı bloklanır.
export default function QuoteSummary({ totalsByCurrency }) {
  const currencies = Object.keys(totalsByCurrency);
  const vatPct = Math.round(VAT_RATE * 100);

  return (
    <aside className="qsum" aria-label="Toplam">
      <h2 className="qsum__title">Toplam</h2>

      {currencies.length === 0 ? (
        <p className="qsum__empty">Henüz ürün seçilmedi.</p>
      ) : (
        currencies.map((cur) => {
          const base = totalsByCurrency[cur];
          const vat = base * VAT_RATE;
          const grand = base + vat;
          return (
            <div key={cur} className="qsum__group">
              <div className="qsum__row">
                <span>Ara Toplam ({cur})</span>
                <span className="mono">{formatPrice(base, cur)}</span>
              </div>
              <div className="qsum__row qsum__row--muted">
                <span>KDV %{vatPct} ({cur})</span>
                <span className="mono">{formatPrice(vat, cur)}</span>
              </div>
              <div className="qsum__row qsum__row--total">
                <span>Genel Toplam ({cur})</span>
                <span className="mono">{formatPrice(grand, cur)}</span>
              </div>
            </div>
          );
        })
      )}
    </aside>
  );
}
