# Ürün Kataloğu — Spring Boot + Thymeleaf

2M Bilişim ürün kataloğu. Önceki React + Vite (frontend) ve Node/Express + Prisma
(backend) mimarisi, **tek çalıştırılabilir JAR** üreten bir **Spring Boot + Thymeleaf**
uygulamasına taşınmıştır. Çalışma anında React / Vite / Node / npm bağımlılığı yoktur.

Uygulama [`product-catalog/`](product-catalog/) altındadır.

## Teknolojiler
Java 21 · Spring Boot 3.3 (Web MVC, Thymeleaf, Data JPA, Security, Validation) ·
Maven · MySQL (`mysql-connector-j`) · Flyway · AWS SDK v2 (S3) · Apache POI (Excel) ·
OpenPDF (teklif PDF) · HTML5 / CSS3 / vanilla JS.

## Özellikler
- **Vitrin:** katalog (arama/marka/kategori filtresi, sıralama, sayfalama), ürün detayı,
  partnerler, teklif sihirbazı, hero & kampanya slider'ları, istemci sepeti + teklif PDF.
- **Kimlik doğrulama:** Spring Security oturum tabanlı form-login; roller CUSTOMER / ADMIN / SUPER_ADMIN.
- **Admin paneli:** dashboard, ürün CRUD + S3 görsel yükleme, kategoriler, kampanya
  banner'ları, fiyat listeleri, teklif yönetimi, müşteriler, kullanıcı yönetimi (SUPER_ADMIN),
  audit log (SUPER_ADMIN), Excel'den ürün içe aktarma.

## Derleme
```bash
cd product-catalog
mvn clean package        # -> target/product-catalog.jar
```

## Çalıştırma (üretim — MySQL)
Ortam değişkenleriyle yapılandırılır (kaynak kodda sır yoktur):
```bash
export DB_URL="jdbc:mysql://localhost:3306/urun_katalog?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC"
export DB_USERNAME=... DB_PASSWORD=...
# Görsel yükleme için S3 (opsiyonel):
export S3_ENDPOINT=... S3_REGION=... S3_BUCKET=... S3_ACCESS_KEY_ID=... S3_SECRET_ACCESS_KEY=... S3_PUBLIC_URL_BASE=...
# Bootstrap süper admin (opsiyonel; varsayılan superadmin@2mbilisim.local / ChangeMe123!):
export APP_SEED_SUPER_ADMIN_EMAIL=... APP_SEED_SUPER_ADMIN_PASSWORD=...

java -jar target/product-catalog.jar
```
İlk açılışta Flyway şemayı oluşturur ve `DataSeeder` 544 ürün + kategoriler + bootstrap
admin yükler (`src/main/resources/seed/*.json`).

## Yerel demo (MySQL olmadan — gömülü H2)
```bash
java -jar target/product-catalog.jar --spring.profiles.active=dev
```

## Sizin sağlamanız gerekenler
- **MySQL** sunucusu + veritabanı (üretim profili için).
- **S3** uyumlu depolama kimlik bilgileri (ürün + kampanya görselleri; verilmezse yükleme
  devre dışı, uygulama çalışır).
- Harici müşteri veritabanı: gerçek yapı bilinmediği için mock adaptör olarak bırakılmıştır.

## Örnek veri
[`product-catalog/samples/liste.xlsx`](product-catalog/samples/liste.xlsx) — admin **Excel
İçe Aktar** ekranı için örnek ürün listesi.
