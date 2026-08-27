package com.ikibm.catalog.dto;

import java.math.BigDecimal;

/** TCMB'den çekilen USD/EUR alış-satış kurlarının JSON gösterimi.
 * SADECE header'daki "Güncel Kur" rozeti içindir — hiçbir fiyat/hesap mantığında KULLANILMAZ.
 * usd/eur alanları veri hiç çekilememişse (ilk açılışta önbellek boşken ağ hatası) null olabilir;
 * istemci bunu "gösterme" olarak yorumlamalı. */
public record ExchangeRateResponse(CurrencyRate usd, CurrencyRate eur, String source) {

    public record CurrencyRate(BigDecimal buying, BigDecimal selling) {
    }

    public static ExchangeRateResponse empty() {
        return new ExchangeRateResponse(null, null, "TCMB");
    }
}
