// Ürün veri kaynağı.
// Veriler `products.json` içinde tutulur ve `scripts/importProducts.js`
// tarafından Excel'den (data/products.xlsx) otomatik üretilir:
//
//   data/products.xlsx  →  npm run import-products  →  src/data/products.json
//
// Alan adları ve yapı, ileride bir REST API'den (örn. GET /api/products)
// dönecek response ile birebir eşleşecek şekilde tasarlandı; backend'e
// geçişte yalnızca bu modülün import kaynağı değişir, tüketen bileşenler
// aynı kalır.

import productsData from "./products.json";

export const products = productsData;
export default productsData;
