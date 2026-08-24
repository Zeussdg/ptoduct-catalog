package com.ikibm.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/** Admin fiyat listesi detayındaki "seçilenleri ekle" formunun payload'ı (JSON). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PriceListItemsUpdateRequest(List<Item> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(Integer productId, BigDecimal price, String currency) {}
}
