package com.ikibm.catalog.util;

import com.ikibm.catalog.entity.Currency;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * React'teki src/utils/format.js formatPrice ile aynı çıktı:
 * sembol + tr-TR gruplamalı, 2 ondalık.  Thymeleaf'te @priceFormatter olarak
 * kullanılır: th:text="${@priceFormatter.format(p.price, p.currency)}".
 */
@Component("priceFormatter")
public class PriceFormatter {

    private final DecimalFormat df;

    public PriceFormatter() {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(new Locale("tr", "TR"));
        sym.setGroupingSeparator('.');
        sym.setDecimalSeparator(',');
        this.df = new DecimalFormat("#,##0.00", sym);
    }

    public String symbol(Currency currency) {
        if (currency == null) return "";
        return switch (currency) {
            case USD -> "$";
            case EUR -> "€";
            case TRY -> "₺";
        };
    }

    public String format(BigDecimal price, Currency currency) {
        if (price == null) return "Fiyat isteyin";
        return symbol(currency) + df.format(price);
    }

    public String format(BigDecimal price, String currency) {
        Currency c;
        try {
            c = currency == null ? Currency.TRY : Currency.valueOf(currency);
        } catch (IllegalArgumentException e) {
            c = Currency.TRY;
        }
        return format(price, c);
    }
}
