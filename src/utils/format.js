const SYMBOLS = { USD: "$", EUR: "€", TRY: "₺" };

export function formatPrice(price, currency = "USD") {
  if (price == null || Number.isNaN(price)) return "Fiyat isteyin";
  const symbol = SYMBOLS[currency] || currency + " ";
  return `${symbol}${Number(price).toLocaleString("tr-TR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}
