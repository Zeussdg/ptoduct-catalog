# Ürün Kataloğu

2M BILISIM ve diğer markaların ürünlerini listeleyen
bağımsız bir B2B ürün katalog uygulaması. Sepet, ödeme veya kampanya gibi
e-ticaret özellikleri içermez; ürünlere hızlı erişim ve fiyat teklifi
hazırlamaya yönelik basit bir sepet/teklif listesi sunar.

Proje ayrıca kendi backend'i (Node.js/Express + Prisma/MySQL), JWT+HttpOnly
cookie tabanlı authentication, rol bazlı yetkilendirme (CUSTOMER / ADMIN /
SUPER_ADMIN) ve bir admin paneli içerir — bkz. [Backend](#backend-server).

## Kurulum ve çalıştırma

```bash
npm install
npm run import-products   # data/products.xlsx → src/data/*.json (varsa)
npm run dev               # geliştirme sunucusu (client, :5173)
npm run build             # production derlemesi (dist/ klasörü)
npm run preview           # production derlemesini yerelde önizle
```

Client, backend API'sine `VITE_API_URL` üzerinden bağlanır (bkz. `.env.example`).
Backend'i ayrıca çalıştırmak gerekir — bkz. [Backend](#backend-server).

## Proje yapısı

```
data/
  products.xlsx           ürün kaynağı (Excel) — buraya konur
scripts/
  importProducts.js       Excel → JSON import pipeline'ı
  categoryMap.js          kategori sınıflandırma kuralları + slug yardımcıları
src/
  components/     Header, HeroSlider, CategorySidebar, ProductCard, ...
  pages/          ProductsPage, ProductDetailPage, PartnersPage, LoginPage, ...
  pages/admin/    Admin paneli sayfaları (Dashboard, Ürünler, Teklifler, ...)
  data/           products.json + categories.json (üretilen), *.js (shim'ler),
                  partners.js, heroSlides.js
  context/        CartContext (misafir sepet/teklif listesi), AuthContext (oturum)
  layouts/        PublicLayout (Header/Footer/CartDrawer), AdminLayout (sidebar)
  routes/         ProtectedRoute, RoleBasedRoute (frontend route guard'ları)
  services/       apiClient + backend API servis modülleri
  styles/         tokens.css (renk, tipografi, layout değişkenleri)
server/
  src/            Express API (controllers/services/routes/middleware/config)
  prisma/         schema.prisma, migrations/, seed.js
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

## Backend (`server/`)

Node.js + Express API, Prisma ORM ile MySQL'e bağlanır. JWT authentication
HttpOnly cookie içinde tutulur; yetkilendirme (CUSTOMER/ADMIN/SUPER_ADMIN)
tamamen backend'de `requireAuth`/`requireRole` middleware'leri ile uygulanır.

### Kurulum

```bash
cd server
npm install
cp .env.example .env      # DATABASE_URL, JWT_*, S3_* değerlerini doldurun
```

`DATABASE_URL`, gerçek bir MySQL sunucusunu göstermelidir (yerel MySQL,
Docker, veya bir bulut MySQL servisi). **Bu ortamda yerel bir MySQL sunucusu
bulunmadığından migration bu oturumda çalıştırılamadı** — `schema.prisma`
`npx prisma validate` ile doğrulandı ve Prisma Client `npx prisma generate`
ile üretildi, ancak gerçek tabloları oluşturmak için aşağıdaki adımı gerçek
bir MySQL bağlantısıyla siz çalıştırmalısınız:

```bash
npx prisma migrate dev --name init   # tabloları oluşturur
npm run prisma:seed                  # src/data/products.json + categories.json'ı
                                      # aktarır, bootstrap bir SUPER_ADMIN oluşturur
                                      # (konsola yazdırılan e-posta/şifre ile giriş yapıp
                                      # şifreyi değiştirin — yalnızca geliştirme amaçlıdır)
npm run dev                          # API sunucusu (:4000)
```

### Harici müşteri veritabanı entegrasyonu — DISCOVERY BEKLİYOR

Mevcut müşterilerin tutulduğu harici veritabanının **gerçek yapısı henüz
bilinmiyor** (database engine, tablo/kolon adları, customer ID tipi, email/
password alanlarının var olup olmadığı, password formatı — hiçbiri
varsayılmadı). `server/src/services/externalCustomer/README.md` dosyası,
discovery aşamasında belirlenmesi gereken tüm noktaları ve gerçek bilgi
geldiğinde izlenecek 13 adımlık süreci listeler. Bu netleşene kadar sistem
`MockExternalCustomerAdapter` (sahte, yalnızca development amaçlı veri)
kullanır ve login akışı yalnızca Product Catalog'un kendi `users` tablosuna
karşı çalışır.

### Ortam değişkenleri

Bkz. `server/.env.example` — MySQL bağlantısı, JWT sırları, S3-uyumlu object
storage (görsel yükleme) ve harici DB (`EXTERNAL_DB_*`, hepsi opsiyonel/boş)
değişkenlerinin tümü orada dokümante edilmiştir. `.env` dosyaları asla
commit edilmez (`.gitignore`'da).
