package com.ikibm.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

/** Admin sipariş düzenleme ekranındaki "Ürün Ekle" aramasının tek satırı — katalog fiyatı ve
 * (varsa) o ürünü içeren aktif fiyat listelerindeki eşleşmeleri de içerir. */
public record OrderProductSearchResult(Integer id, String name, String brand, String stockCode,
                                       BigDecimal price, String currency, List<ListMatch> listMatches) {

    public record ListMatch(String priceListName, BigDecimal price, String currency) {}
}
