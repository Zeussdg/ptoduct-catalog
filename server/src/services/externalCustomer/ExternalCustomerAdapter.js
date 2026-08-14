/**
 * External Customer Adapter — KAVRAMSAL ARAYÜZ.
 *
 * Harici müşteri veritabanının gerçek yapısı (database engine, tablo/kolon
 * adları, customer ID tipi, email alanının var/unique olup olmadığı,
 * password formatı, authentication yöntemi) şu an UNKNOWN'dur ve
 * varsayılmamıştır. Detaylar için bkz. ./README.md ve proje mimari planının
 * "External Database Entegrasyonu" bölümü.
 *
 * Bu sınıf, gerçek adapter'lar için ortak taban ve dokümantasyon amaçlıdır.
 * Metod imzaları KESİN DEĞİLDİR — discovery aşaması tamamlanmadan hangi
 * lookup yönteminin (email, customerCode, externalId, ...) kullanılacağı
 * belirlenemez. `findByEmail` gibi tek bir metodu zorunlu kılmak, email
 * alanının var olduğunu/unique olduğunu varsaymak anlamına gelirdi — bu
 * varsayım kesinlikle yapılmamaktadır.
 *
 * @typedef {Object} NormalizedExternalCustomer
 * @property {string} externalId - Kaynaktaki ham ID, adapter içinde string'e çevrilir.
 * @property {string} source - Kaynak sistemin adı (örn. "legacy-erp") — TBD.
 * @property {string|null} email
 * @property {string|null} firstName
 * @property {string|null} lastName
 * @property {string|null} companyName
 * @property {string|null} phone
 * @property {string|null} status - Ham durum bilgisi; yorumlanması TBD.
 */
export class ExternalCustomerAdapter {
  /**
   * Discovery sonrası eklenmesi muhtemel yöntemlerden biri. Şu an implement
   * EDİLMEMİŞTİR — sadece kontratın şeklini belgelemek içindir.
   * @param {string} _email
   * @returns {Promise<NormalizedExternalCustomer|null>}
   */
  async findByEmail(_email) {
    throw new Error("ExternalCustomerAdapter.findByEmail: discovery tamamlanmadan implement edilmedi");
  }

  /**
   * @param {string} _externalId
   * @returns {Promise<NormalizedExternalCustomer|null>}
   */
  async findByExternalId(_externalId) {
    throw new Error("ExternalCustomerAdapter.findByExternalId: discovery tamamlanmadan implement edilmedi");
  }
}
