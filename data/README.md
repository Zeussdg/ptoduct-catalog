# Ürün verisi (Excel) klasörü

Ürünler bu klasöre konulan bir Excel dosyasından otomatik içe aktarılır.

## Kullanım

1. Excel dosyanızı bu klasöre şu adlardan biriyle koyun:

   ```
   data/products.xlsx
   data/urunler.xlsx
   data/products/urunler.xlsx   (alt klasör de olur)
   ```

2. İçe aktarma script'ini çalıştırın:

   ```bash
   npm run import-products
   ```

3. Uygulamayı çalıştırın:

   ```bash
   npm run dev
   ```

Script; `src/data/products.json` ve `src/data/categories.json` dosyalarını
yeniden üretir. React uygulaması bu JSON'ları kullanır.

## Excel sütunları

Sütun **başlıkları otomatik tespit edilir** (büyük/küçük harf ve Türkçe
karakter duyarsız). Aşağıdaki başlıklardan bilinenler tanınır:

| Alan            | Tanınan başlıklar (örnek)                                   |
| --------------- | ----------------------------------------------------------- |
| Marka           | Marka, Brand, Üretici                                       |
| Ürün Adı        | Ürün Adı, Malzeme Adı, Stok Adı, Ürün, Tanım, Name          |
| Stok Kodu       | Stok Kodu, Ürün Kodu, Malzeme Kodu, Barkod, SKU, Kod        |
| Kategori        | Kategori, Alt Kategori, Ürün Grubu, Grup, Tip               |
| Fiyat           | Fiyat, Liste Fiyatı, Birim Fiyat, Satış Fiyatı, Tutar       |
| Para Birimi     | Para Birimi, Döviz, Currency, Kur                           |
| Açıklama        | Açıklama, Description, Detay, Özellik                        |

- Yalnızca **Ürün Adı** zorunludur; diğerleri eksikse makul varsayımlar
  uygulanır (marka ad'dan türetilir, fiyat boş bırakılır vb.).
- Fiyat hem `1.234,56` (TR) hem `1,234.56` (EN) biçimini, para birimi hem
  `$ / € / ₺` sembollerini hem `USD / EUR / TRY` metnini tanır.
- Aynı ürünler (stok kodu ya da marka+ad) otomatik tekilleştirilir.

## Kategori eşleme

Ham kategori metinleri + ürün adı, `scripts/categoryMap.js` içindeki anahtar
kelime kurallarıyla ana kategori / alt kategori hiyerarşisine eşlenir. Yeni
bir ürün tipi "Diğer Ürünler" altına düşerse, ilgili anahtar kelimeyi bu
dosyaya eklemek yeterlidir.
