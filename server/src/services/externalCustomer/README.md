# External Customer Adapter — Discovery Dokümantasyonu

> External database schema, customer identifiers, authentication structure and
> password storage format are currently unknown. These details must not be
> assumed during initial development. They will be determined during the
> external database discovery and mapping phase.

Bu klasör, henüz bilinmeyen bir harici müşteri veritabanına bağlanmak için
tasarlanmış izolasyon katmanını içerir. Gerçek bağlantı bilgisi ve şema
netleşene kadar `MockExternalCustomerAdapter` kullanılır.

## Şu an bilinmeyenler (UNKNOWN)

- Database engine (MySQL / SQL Server / PostgreSQL / Oracle / başka)
- Database/schema adı
- Customer/müşteri tablosunun adı, primary key / foreign key yapısı
- Customer ID alanının adı ve tipi (int/uuid/string)
- Email alanının var olup olmadığı, unique olup olmadığı
- Kullanıcı/login tablosunun ayrı olup olmadığı, kullanıcı ↔ müşteri ilişkisi
- Password alanının var olup olmadığı, formatı/hash algoritması
- Firma/adres/telefon bilgilerinin hangi tablo(lar)da olduğu
- Customer ↔ Company/Cari/Firma kavram eşleşmesi
- Senkronizasyon stratejisi ve sıklığı

## Discovery tamamlanmadan yapılmayacaklar

- Gerçek bir `MySqlExternalCustomerAdapter` (veya başka bir engine'e özel adapter) yazılmayacak.
- `authService` login akışına harici DB fallback'i eklenmeyecek.
- Herhangi bir password hashing/verifying stratejisi (bcrypt/md5/sha1/plaintext) varsayılmayacak.

## Gerçek bilgi geldiğinde izlenecek süreç

1. Database bağlantısını test et.
2. Database engine ve schema yapısını belirle.
3. Tabloları ve kolonları analiz et.
4. Customer/müşteri kayıtlarının nerede tutulduğunu belirle.
5. Customer ID alanını belirle.
6. Authentication/login bilgilerinin nerede tutulduğunu belirle.
7. Password formatını belirle.
8. Customer → Company/User ilişkilerini belirle.
9. External → Internal field mapping oluştur (bu README'ye eklenir).
10. `ExternalCustomerAdapter` implementasyonunu (gerçek DB'ye özel) oluştur.
11. Authentication entegrasyonunu gerçek yapıya göre oluştur (`authService.login` içine, jenerik hata mesajları koruyarak).
12. Test kullanıcılarıyla entegrasyonu test et.
13. Production'a geçmeden önce security review yap.

## Normalize model (kesin — Product Catalog tarafı)

Harici sistemin yapısı ne olursa olsun, adapter dışarıya her zaman
`NormalizedExternalCustomer` şeklini döndürecek (bkz. `ExternalCustomerAdapter.js`):

```
{ externalId, source, email, firstName, lastName, companyName, phone, status }
```

Bu model `external_identities` (`source` + `externalId`, unique) ve `users`
tablolarına map edilir. Gerçek kaynak alan adları netleşince, bu mapping
mantığı yalnızca yeni adapter implementasyonunun içinde yaşayacak — geri
kalan uygulama hiç değişmeyecek.
