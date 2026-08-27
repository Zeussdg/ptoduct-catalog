package com.ikibm.catalog.controller;

import com.ikibm.catalog.dto.ExchangeRateResponse;
import com.ikibm.catalog.service.ExchangeRateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Header'daki "Güncel Kur" rozeti için TCMB USD/EUR kurlarını döner — salt gösterim amaçlıdır,
 * hiçbir fiyat/hesap hesaplamasında kullanılmaz. */
@RestController
public class ExchangeRateApiController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateApiController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping("/api/exchange-rates")
    public ExchangeRateResponse rates() {
        return exchangeRateService.getRates();
    }
}
