// Kategori hiyerarşisi.
// Veriler `categories.json` içinde tutulur ve `scripts/importProducts.js`
// tarafından Excel içeriği analiz edilerek otomatik üretilir. Ham kategori
// adları `scripts/categoryMap.js` üzerinden ana kategori / alt kategori
// hiyerarşisine eşlenir.

import categoriesData from "./categories.json";

export const categories = categoriesData;
export default categoriesData;
