import { createContext, useContext, useMemo, useState, useCallback } from "react";

const CartContext = createContext(null);

export function CartProvider({ children }) {
  const [items, setItems] = useState([]); // { product, qty }
  const [isOpen, setIsOpen] = useState(false);

  const addItem = useCallback((product, qty = 1) => {
    setItems((prev) => {
      const existing = prev.find((it) => it.product.id === product.id);
      if (existing) {
        return prev.map((it) =>
          it.product.id === product.id ? { ...it, qty: it.qty + qty } : it
        );
      }
      return [...prev, { product, qty }];
    });
    setIsOpen(true);
  }, []);

  const removeItem = useCallback((productId) => {
    setItems((prev) => prev.filter((it) => it.product.id !== productId));
  }, []);

  const setQty = useCallback((productId, qty) => {
    setItems((prev) => {
      if (qty <= 0) return prev.filter((it) => it.product.id !== productId);
      return prev.map((it) =>
        it.product.id === productId ? { ...it, qty } : it
      );
    });
  }, []);

  const clearCart = useCallback(() => setItems([]), []);
  const openCart = useCallback(() => setIsOpen(true), []);
  const closeCart = useCallback(() => setIsOpen(false), []);

  const totalCount = useMemo(
    () => items.reduce((sum, it) => sum + it.qty, 0),
    [items]
  );

  // Prices come in mixed currencies (USD / EUR) — never sum across
  // currencies, group totals per currency instead.
  const totalsByCurrency = useMemo(() => {
    const totals = {};
    for (const it of items) {
      if (it.product.price == null) continue;
      const cur = it.product.currency || "USD";
      totals[cur] = (totals[cur] || 0) + it.product.price * it.qty;
    }
    return totals;
  }, [items]);

  const value = {
    items,
    isOpen,
    addItem,
    removeItem,
    setQty,
    clearCart,
    openCart,
    closeCart,
    totalCount,
    totalsByCurrency,
  };

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error("useCart must be used within CartProvider");
  return ctx;
}
