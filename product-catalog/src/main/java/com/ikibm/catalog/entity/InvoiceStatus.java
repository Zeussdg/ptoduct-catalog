package com.ikibm.catalog.entity;

/** DRAFT şu an hiçbir akıştan tetiklenmiyor (ileride kullanılabilecek hazır alan) — "Faturalandır"
 * butonu doğrudan ISSUED oluşturur, MailTemplateStatus.ARCHIVED ile aynı desen. */
public enum InvoiceStatus {
    DRAFT,
    ISSUED,
    CANCELLED
}
