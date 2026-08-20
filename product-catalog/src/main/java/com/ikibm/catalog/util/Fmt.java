package com.ikibm.catalog.util;

import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.entity.Order;
import com.ikibm.catalog.entity.OrderItem;
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
    private static final BigDecimal VAT = new BigDecimal("0.20");

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
        Map<Currency, BigDecimal> sums = perCurrencySubtotal(quote);
        if (sums.isEmpty()) return "-";
        return sums.entrySet().stream()
                .map(e -> priceFormatter.format(e.getValue(), e.getKey()))
                .collect(Collectors.joining(" + "));
    }

    /** Kâr marjı uygulanmış ve %20 KDV dahil genel toplam (müşterinin "Tekliflerim" sayfası için —
     * bu teklif toptancıya değil müşterinin kendi 3. taraf müşterisine yapıldığından gösterilecek
     * gerçek tutar budur, PDF'teki "Genel Toplam" ile birebir aynı hesap). */
    public String quoteGrandTotal(Quote quote) {
        Map<Currency, BigDecimal> sums = perCurrencySubtotal(quote);
        if (sums.isEmpty()) return "-";
        BigDecimal factor = BigDecimal.ONE.add(marginFraction(quote));
        return sums.entrySet().stream()
                .map(e -> {
                    BigDecimal afterMargin = e.getValue().multiply(factor);
                    BigDecimal grand = afterMargin.add(afterMargin.multiply(VAT));
                    return priceFormatter.format(grand, e.getKey());
                })
                .collect(Collectors.joining(" + "));
    }

    /** Kâr marjı yüzdesi + tutarı (ör. "%20 (+$4,63)"); marj yoksa "-" döner. */
    public String quoteMargin(Quote quote) {
        BigDecimal pct = quote.getMarginPct();
        if (pct == null || pct.signum() == 0) return "-";
        Map<Currency, BigDecimal> sums = perCurrencySubtotal(quote);
        if (sums.isEmpty()) return "-";
        BigDecimal fraction = marginFraction(quote);
        String amounts = sums.entrySet().stream()
                .map(e -> priceFormatter.format(e.getValue().multiply(fraction), e.getKey()))
                .collect(Collectors.joining(" + "));
        return "%" + pct.stripTrailingZeros().toPlainString() + " (+" + amounts + ")";
    }

    private BigDecimal marginFraction(Quote quote) {
        BigDecimal pct = quote.getMarginPct();
        return pct == null ? BigDecimal.ZERO : pct.divide(BigDecimal.valueOf(100));
    }

    private Map<Currency, BigDecimal> perCurrencySubtotal(Quote quote) {
        List<QuoteItem> items = quote.getItems();
        Map<Currency, BigDecimal> sums = new EnumMap<>(Currency.class);
        if (items == null) return sums;
        for (QuoteItem it : items) {
            sums.merge(it.getCurrency(), it.getTotalPrice(), BigDecimal::add);
        }
        return sums;
    }

    /** Bir siparişin kalemlerini para birimine göre toplayıp " + " ile birleştirir. */
    public String orderTotal(Order order) {
        Map<Currency, BigDecimal> sums = perCurrencySubtotal(order);
        if (sums.isEmpty()) return "-";
        return sums.entrySet().stream()
                .map(e -> priceFormatter.format(e.getValue(), e.getKey()))
                .collect(Collectors.joining(" + "));
    }

    /** Sipariş tutarı: ham (toptancı) fiyat + %20 KDV — kâr marjı içermez, bayi bize
     * kendi müşterisine uyguladığı marjı değil, mal bedelini öder. */
    public String orderGrandTotal(Order order) {
        Map<Currency, BigDecimal> sums = perCurrencySubtotal(order);
        if (sums.isEmpty()) return "-";
        return sums.entrySet().stream()
                .map(e -> {
                    BigDecimal grand = e.getValue().add(e.getValue().multiply(VAT));
                    return priceFormatter.format(grand, e.getKey());
                })
                .collect(Collectors.joining(" + "));
    }

    private Map<Currency, BigDecimal> perCurrencySubtotal(Order order) {
        List<OrderItem> items = order.getItems();
        Map<Currency, BigDecimal> sums = new EnumMap<>(Currency.class);
        if (items == null) return sums;
        for (OrderItem it : items) {
            sums.merge(it.getCurrency(), it.getTotalPrice(), BigDecimal::add);
        }
        return sums;
    }
}
