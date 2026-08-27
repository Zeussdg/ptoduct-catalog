package com.ikibm.catalog.dto;

import com.ikibm.catalog.entity.Currency;

import java.math.BigDecimal;

/** Bir faturanın tek bir para birimindeki ara toplam/KDV/genel toplam kırılımı — sipariş gibi bir
 * fatura da farklı para birimlerinden kalemler içerebildiği için Invoice üzerinde tek bir grandTotal
 * alanı yoktur, bunun yerine para birimi başına bu özet üretilir (grandTotal her zaman subtotal+vatAmount). */
public record InvoiceCurrencySummary(Currency currency, BigDecimal subtotal, BigDecimal vatAmount, BigDecimal grandTotal) {
}
