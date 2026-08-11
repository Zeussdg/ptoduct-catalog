# Ürün Kataloğu

TENDA · TELESIS · HUAWEI · HIKVISION ve diğer markaların ürünlerini listeleyen
bağımsız bir B2B ürün katalog uygulaması. Sepet, ödeme veya kampanya gibi
e-ticaret özellikleri içermez; ürünlere hızlı erişim ve fiyat teklifi
hazırlamaya yönelik basit bir sepet/teklif listesi sunar.

## Kurulum ve çalıştırma

```bash
npm install
npm run import-products   # data/products.xlsx → src/data/*.json (varsa)
npm run dev               # geliştirme sunucusu
npm run build             # production derlemesi (dist/ klasörü)
npm run preview           # production derlemesini yerelde önizle
```

## Proje yapısı

```
data/
  products.xlsx           ürün kaynağı (Excel) — buraya konur
scripts/
  importProducts.js       Excel → JSON import pipeline'ı
  categoryMap.js          kategori sınıflandırma kuralları + slug yardımcıları
src/
  components/     Header, HeroSlider, CategorySidebar, ProductCard, ...
  pages/          ProductsPage, ProductDetailPage, PartnersPage
  data/           products.json + categories.json (üretilen), *.js (shim'ler),
                  partners.js, heroSlides.js
  context/        CartContext (sepet state'i)
  styles/         tokens.css (renk, tipografi, layout değişkenleri)
```

## Excel'den ürün içe aktarma (Excel → JSON → React)

Ürünler React koduna elle yazılmaz; bir Excel dosyasından build-time'da
içe aktarılır:

```
data/products.xlsx  →  npm run import-products  →  src/data/products.json
                                                →  src/data/categories.json
                                                →  React Product Catalog
```

`scripts/importProducts.js`:

1. Excel dosyasını bulur (`data/products.xlsx` ya da `data/products/*.xlsx`)
2. Başlık satırını ve sütunları otomatik tespit eder (Türkçe başlık eşleme)
3. Satırları normalize eder (TR/EN ondalık fiyat, `$ / € / ₺` para birimi,
   metin temizliği, marka türetme)
4. Ham kategori + ürün adını `categoryMap.js` kurallarıyla ana/alt
   kategoriye eşler
5. Aynı ürünleri (stok kodu ya da marka+ad) tekilleştirir
6. `products.json` + `categories.json` üretir
7. Konsola analiz raporu basar (sütun eşleşmeleri, kategori dağılımı,
   tekrar sayısı, sınıflandırılamayan ürünler)

Excel bulunamazsa mevcut JSON'lara dokunulmaz; uygulama çalışmaya devam
eder. Yeni ürün listesi geldiğinde tek yapılması gereken: **Excel'i
`data/` klasörüne koymak, `npm run import-products` çalıştırmak, uygulamayı
başlatmak.** Ayrıntı ve tanınan sütun başlıkları için `data/README.md`.

## Veri kaynağı

Ürünler `src/data/products.json` içinde tutulur; alan adları (`brand`,
`name`, `stockCode`, `mainCategory`, `category`, `price`, `currency`,
`description`, `image`) ileride bir REST API'den (`GET /api/products`)
dönecek response ile birebir eşleşecek şekilde tasarlandı. `src/data/
products.js` yalnızca bu JSON'u yeniden dışa aktaran ince bir shim'dir;
backend'e geçişte sadece bu modülün import kaynağı değişir, component'ler
değişmeden kalır.

## Sayfalar & bileşenler

- **Hero slider** (`HeroSlider`): ana sayfada, ürün gruplarını temsil eden
  otomatik geçişli / ok + dot navigasyonlu / sonsuz döngülü, responsive
  carousel. Her slayt ilgili kategori filtresine yönlendirir.
- **Navbar** (`Header`): Ürünler · İş Ortaklarımız · Sepet.
- **Kampanya slider'ı** (`CampaignSlider`): ana sayfada, hero'nun hemen
  altında ve kategori sidebar'ı (`catsb`) ile yan yana — ürün içerik
  kolonunun üstünde gösterilir. Büyük banner; otomatik geçiş, ok + dot
  navigasyonu, sonsuz döngü, touch/swipe ve responsive.
- **İş Ortaklarımız** (`/is-ortaklarimiz`): çözüm ortağı markalar; logo
  yoksa firma adının baş harflerinden placeholder üretilir.
- **Ürün listesi sayfalama**: "Tüm Ürünler" ve tüm filtreli görünümler
  sayfa başına **50 ürün** gösterir; sayfa numarası URL'de `?sayfa=`
  parametresiyle taşınır, filtre/arama değişince 1. sayfaya döner
  (`Pagination`).

## Notlar

- Kategori menüsü tamamen metin tabanlı, görsel kart kullanmaz.
- Arama; ürün adı, marka, stok kodu ve kategoriye göre çalışır, kategori
  filtresiyle birlikte kullanılabilir.
- Sepet fiyatları döviz bazında (USD / EUR) ayrı toplanır — HUAWEI ürünleri
  EUR, diğerleri çoğunlukla USD fiyatlıdır, karışık toplama yapılmaz.
- Ürün görselleri şu an `null`; `image` alanına gerçek görsel URL'i
  eklendiğinde `ProductCard` ve `ProductDetailPage` otomatik gösterecek
  şekilde hazır (şu an marka adı ile placeholder gösteriliyor).
