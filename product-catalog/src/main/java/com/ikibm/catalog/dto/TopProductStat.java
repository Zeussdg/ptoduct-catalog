package com.ikibm.catalog.dto;

/** En çok teklif edilen ürünler listesi için tek bir ürün satırı (teklif anındaki snapshot ad/kod). */
public record TopProductStat(String productName, String productCode, long offerCount, long totalQty) {
}
