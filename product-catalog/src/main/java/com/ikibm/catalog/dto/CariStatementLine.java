package com.ikibm.catalog.dto;

import com.ikibm.catalog.entity.CariTransaction;
import com.ikibm.catalog.entity.Invoice;
import com.ikibm.catalog.entity.Order;

/** Cari ekstre PDF'inde tek bir satır — hareketin kendisi + (detaylı modda, referansı çözülebiliyorsa)
 * arkasındaki gerçek Sipariş/Fatura kaydı. order/invoice sadece biri dolu olabilir, ikisi de null olabilir
 * (MANUAL referanslı hareketler, ya da özet modda hiç çözülmediği için). */
public record CariStatementLine(CariTransaction transaction, Order order, Invoice invoice) {
}
