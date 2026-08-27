package com.ikibm.catalog.entity;

/** Cari hareketin kaynağı — sipariş/teklif/fatura/manuel ile yumuşak (FK'siz) ilişki için.
 * INVOICE: InvoiceService tarafından fatura oluşturma/iptal akışında yazılan DEBT/ADJUSTMENT hareketleri
 * (referenceId = Invoice.id) — cari borç artık sadece bu yoldan oluşur. ORDER: artık yeni kayıt
 * yazılmıyor (sipariş oluşturulduğunda otomatik borçlanma kaldırıldı), ama geçmişte bu şekilde
 * yazılmış kayıtlar DB'de olduğu gibi duruyor, bu değer sadece o geçmiş veriyi okuyabilmek için tutuluyor. */
public enum CariReferenceType {
    ORDER, QUOTE, MANUAL, INVOICE
}
