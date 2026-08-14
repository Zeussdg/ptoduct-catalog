package com.ikibm.catalog.dto;

import com.ikibm.catalog.entity.Product;

import java.math.BigDecimal;

/** Teklif sihirbazı combobox'ı için hafif ürün gösterimi (JSON). */
public record ProductLite(Integer id, String brand, String name, String stockCode,
                          BigDecimal price, String currency) {

    public static ProductLite of(Product p) {
        return new ProductLite(p.getId(), p.getBrand(), p.getName(), p.getStockCode(),
                p.getPrice(), p.getCurrency() != null ? p.getCurrency().name() : "TRY");
    }
}
