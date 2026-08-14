package com.ikibm.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SeedProduct(
        Integer id,
        String brand,
        String name,
        String stockCode,
        String mainCategory,
        String mainCategorySlug,
        String category,
        String categorySlug,
        BigDecimal price,
        String currency,
        String description,
        String image
) {
}
