package com.ikibm.catalog.dto;

import com.ikibm.catalog.entity.Currency;

import java.math.BigDecimal;

/** Bir ürünün, verilen kullanıcı için çözümlenmiş fiyatı (müşteriye özel fiyat varsa onu yansıtır). */
public record ResolvedPrice(BigDecimal price, Currency currency, boolean customerSpecific) {
}
