package com.ikibm.catalog.util;

import com.ikibm.catalog.dto.InvoiceCurrencySummary;
import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.entity.InvoiceItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Fatura kalemlerini para birimine göre ayrı ayrı toplar — farklı para birimleri KESİNLİKLE
 * birbirine eklenmez (mevcut PdfService/Fmt'in sipariş/teklif için uyguladığı mantıkla aynı ilke).
 * Saf/stateless fonksiyon: hem InvoiceService (cari borç tutarlarını hesaplamak için) hem
 * InvoicePdfService (PDF özet tablosu için) hem de Thymeleaf şablonları (T(...) static çağrısıyla)
 * tarafından kullanılır. */
public final class InvoiceCalculator {

    private InvoiceCalculator() {
    }

    /** Her satırın ara toplamı (KDV hariç net karşılığı) + KDV tutarını para birimine göre toplar.
     * grandTotal = subtotal + vatAmount, her zaman satırların gerçek KDV dahil tutarlarının toplamına eşittir. */
    public static List<InvoiceCurrencySummary> summarize(List<InvoiceItem> items) {
        Map<Currency, BigDecimal> subtotals = new EnumMap<>(Currency.class);
        Map<Currency, BigDecimal> vats = new EnumMap<>(Currency.class);
        if (items != null) {
            for (InvoiceItem item : items) {
                BigDecimal subtotal = Boolean.TRUE.equals(item.getPriceIncludesVat())
                        ? item.getLineTotal().subtract(item.getVatAmount())
                        : item.getLineTotal();
                subtotals.merge(item.getCurrency(), subtotal, BigDecimal::add);
                vats.merge(item.getCurrency(), item.getVatAmount(), BigDecimal::add);
            }
        }
        List<InvoiceCurrencySummary> result = new ArrayList<>();
        for (Map.Entry<Currency, BigDecimal> entry : subtotals.entrySet()) {
            BigDecimal subtotal = entry.getValue();
            BigDecimal vat = vats.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            result.add(new InvoiceCurrencySummary(entry.getKey(), subtotal, vat, subtotal.add(vat)));
        }
        result.sort(Comparator.comparing(InvoiceCurrencySummary::currency));
        return result;
    }

    /** Faturanın cari hesaba yazılacak borç tutarları — para birimi başına, satırların KDV dahil
     * (gerçekte ödenmesi gereken) toplamı üzerinden. */
    public static Map<Currency, BigDecimal> debtAmountsByCurrency(List<InvoiceItem> items) {
        Map<Currency, BigDecimal> result = new EnumMap<>(Currency.class);
        for (InvoiceCurrencySummary s : summarize(items)) {
            result.put(s.currency(), s.grandTotal());
        }
        return result;
    }
}
