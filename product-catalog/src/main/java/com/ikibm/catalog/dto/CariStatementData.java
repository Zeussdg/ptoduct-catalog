package com.ikibm.catalog.dto;

import com.ikibm.catalog.entity.CariAccount;
import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Cari ekstre PDF'i için CariStatementService tarafından hazırlanan, tamamen çözülmüş veri seti —
 * CariStatementPdfService bu veriden başka hiçbir sorgu yapmaz (render-only). totalDebtByCurrency/
 * totalCreditByCurrency, gösterilen (filtrelenmiş) hareketlerin gerçek tutarlarının PARA BİRİMİ
 * BAŞINA toplamıdır — cari-detail.html'deki Borç/Alacak kolon mantığıyla birebir aynı sınıflandırma
 * (DEBT+ADJUSTMENT = Borç, PAYMENT+REFUND = Alacak). Yeni bir bakiye hesaplama mantığı İÇERMEZ;
 * satır bazında balanceAfter ve currentBalance her zaman CariAccountService'in yazdığı gerçek
 * değerlerdir. */
public record CariStatementData(
        User customer,
        CariAccount account,
        Instant reportGeneratedAt,
        Instant filterFrom,
        Instant filterTo,
        boolean detailed,
        List<CariStatementLine> lines,
        Map<Currency, BigDecimal> totalDebtByCurrency,
        Map<Currency, BigDecimal> totalCreditByCurrency) {
}
