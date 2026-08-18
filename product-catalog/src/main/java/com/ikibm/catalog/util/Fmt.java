package com.ikibm.catalog.util;

import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.entity.Quote;
import com.ikibm.catalog.entity.QuoteItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Tarih/toplam biçimleme yardımcısı. Thymeleaf'te @fmt.date(...) / @fmt.quoteTotal(...). */
@Component("fmt")
public class Fmt {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", new Locale("tr", "TR"));
    private static final DateTimeFormatter DATETIME =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", new Locale("tr", "TR"));

    private final PriceFormatter priceFormatter;

    public Fmt(PriceFormatter priceFormatter) {
        this.priceFormatter = priceFormatter;
    }

    public String date(Instant instant) {
        return instant == null ? "" : DATE.format(instant.atZone(ZONE));
    }

    public String dateTime(Instant instant) {
        return instant == null ? "" : DATETIME.format(instant.atZone(ZONE));
    }

    /** Bir teklifin kalemlerini para birimine göre toplayıp " + " ile birleştirir (ör. ₺2.880,00 + $360,00). */
    public String quoteTotal(Quote quote) {
        List<QuoteItem> items = quote.getItems();
        if (items == null || items.isEmpty()) return "-";
        Map<Currency, BigDecimal> sums = new EnumMap<>(Currency.class);
        for (QuoteItem it : items) {
            sums.merge(it.getCurrency(), it.getTotalPrice(), BigDecimal::add);
        }
        return sums.entrySet().stream()
                .map(e -> priceFormatter.format(e.getValue(), e.getKey()))
                .collect(Collectors.joining(" + "));
    }
}
