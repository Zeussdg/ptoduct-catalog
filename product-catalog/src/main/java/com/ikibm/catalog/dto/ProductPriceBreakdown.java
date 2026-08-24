package com.ikibm.catalog.dto;

import com.ikibm.catalog.entity.Currency;

import java.math.BigDecimal;
import java.util.List;

/** Ürün detay sayfasının fiyat kartı için: vurgulanan fiyat + karşılaştırma kademeleri (Özel/Bayi/Liste)
 * + müşterinin bağlı olduğu birden fazla fiyat listesinden gelen seçenekler (ör. Taksitli/Peşin). */
public record ProductPriceBreakdown(
        BigDecimal effectivePrice,
        Currency effectiveCurrency,
        PriceSource source,
        boolean vatIncluded,
        List<PriceListOption> priceListOptions,
        TierAmount special,
        TierAmount dealer,
        TierAmount list
) {
    public record TierAmount(BigDecimal price, Currency currency) {}

    /** Müşterinin bağlı olduğu bir fiyat listesinden bu ürün için gelen fiyat seçeneği — ucuzdan pahalıya sıralı. */
    public record PriceListOption(Integer priceListId, String priceListName, BigDecimal price, Currency currency) {}

    public String badgeLabel() {
        return switch (source) {
            case CUSTOMER -> "Müşterinize Özel";
            case PRICE_LIST -> priceListOptions.isEmpty() ? "Fiyat Listesi" : "Fiyat Listesi: " + priceListOptions.get(0).priceListName();
            case SPECIAL -> "Özel Fiyat";
            case DEALER -> "Bayi Fiyatı";
            case LIST -> "Liste Fiyatı";
        };
    }
}
