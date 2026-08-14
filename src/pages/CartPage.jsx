import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { cartApi } from "../services/cartApi";
import { quotesApi } from "../services/quotesApi";
import { formatPrice } from "../utils/format";
import "./customerPages.css";

export default function CartPage() {
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(true);
  const [creatingQuote, setCreatingQuote] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    cartApi
      .get()
      .then(({ cart }) => setCart(cart))
      .catch(() => setError("Sepet yüklenemedi"))
      .finally(() => setLoading(false));
  }, []);

  async function updateQty(itemId, qty) {
    if (qty < 1) return;
    const { cart: updated } = await cartApi.updateItem(itemId, qty);
    setCart(updated);
  }

  async function removeItem(itemId) {
    const { cart: updated } = await cartApi.removeItem(itemId);
    setCart(updated);
  }

  async function handleCreateQuote() {
    setCreatingQuote(true);
    setError("");
    try {
      const { quote } = await quotesApi.create();
      navigate(`/quotes/${quote.id}`);
    } catch {
      setError("Teklif oluşturulamadı");
    } finally {
      setCreatingQuote(false);
    }
  }

  if (loading) return <div className="custpg">Yükleniyor...</div>;

  const items = cart?.items ?? [];

  return (
    <div className="custpg">
      <h1 className="custpg__title">Sepetim</h1>
      {error && <p style={{ color: "var(--danger-600)" }}>{error}</p>}

      <div className="custpg__card">
        {items.length === 0 ? (
          <div className="custpg__empty">Sepetiniz boş.</div>
        ) : (
          <table className="custpg__table">
            <thead>
              <tr>
                <th>Ürün</th>
                <th>Adet</th>
                <th>Birim Fiyat</th>
                <th>Toplam</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id}>
                  <td>{item.product?.name}</td>
                  <td>
                    <input
                      type="number"
                      min={1}
                      className="custpg__qty-input"
                      value={item.qty}
                      onChange={(e) => updateQty(item.id, Number(e.target.value))}
                    />
                  </td>
                  <td>{formatPrice(item.unitPrice, item.currency)}</td>
                  <td>{formatPrice(item.unitPrice * item.qty, item.currency)}</td>
                  <td>
                    <button className="custpg__btn custpg__btn--danger" onClick={() => removeItem(item.id)}>
                      Sil
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {items.length > 0 && (
        <button className="custpg__btn" onClick={handleCreateQuote} disabled={creatingQuote}>
          {creatingQuote ? "Oluşturuluyor..." : "Teklif İste"}
        </button>
      )}
    </div>
  );
}
