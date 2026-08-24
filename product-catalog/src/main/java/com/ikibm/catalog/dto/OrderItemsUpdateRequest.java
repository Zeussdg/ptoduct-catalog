package com.ikibm.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/** Admin sipariş detayındaki kalem düzenleme formunun payload'ı (JSON). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderItemsUpdateRequest(List<Line> lines) {

    /** itemId doluysa mevcut kalem (sadece qty/unitPrice/currency değişir); boşsa productId ile yeni kalem eklenir.
     * priceIncludesVat true ise fiyat bir fiyat listesinden (KDV dahil) seçilmiştir. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Line(Integer itemId, Integer productId, Integer qty, BigDecimal unitPrice, String currency,
                       Boolean priceIncludesVat) {}
}
