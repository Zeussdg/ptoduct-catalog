package com.ikibm.catalog.service;

import com.ikibm.catalog.config.AppProperties;
import com.ikibm.catalog.dto.ExchangeRateResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** TCMB XML ayrıştırma mantığını gerçeğe yakın örnek feed'lerle doğrular — ağ çağrısı gerekmez
 * (StorageServiceTest'teki gibi gerçek sınıfı doğrudan örnekleyip saf mantığı test etme deseni). */
class ExchangeRateServiceTest {

    private final ExchangeRateService service =
            new ExchangeRateService(RestClient.builder(), new AppProperties());

    private static final String SAMPLE_XML = "<Tarih_Date>"
            + "<Currency Kod=\"USD\"><ForexBuying>48.0292</ForexBuying><ForexSelling>48.1157</ForexSelling></Currency>"
            + "<Currency Kod=\"EUR\"><ForexBuying>56.0388</ForexBuying><ForexSelling>56.1398</ForexSelling></Currency>"
            + "</Tarih_Date>";

    private static final String SAMPLE_XML_NO_EUR = "<Tarih_Date>"
            + "<Currency Kod=\"USD\"><ForexBuying>48.0292</ForexBuying><ForexSelling>48.1157</ForexSelling></Currency>"
            + "</Tarih_Date>";

    @Test
    void parseXml_validFeed_extractsUsdAndEur() throws Exception {
        ExchangeRateResponse r = service.parseXml(SAMPLE_XML);

        assertThat(r.usd().buying()).isEqualByComparingTo("48.0292");
        assertThat(r.usd().selling()).isEqualByComparingTo("48.1157");
        assertThat(r.eur().buying()).isEqualByComparingTo("56.0388");
        assertThat(r.eur().selling()).isEqualByComparingTo("56.1398");
        assertThat(r.source()).isEqualTo("TCMB");
    }

    @Test
    void parseXml_missingCurrency_returnsNullForThatCurrencyOnly() throws Exception {
        ExchangeRateResponse r = service.parseXml(SAMPLE_XML_NO_EUR);

        assertThat(r.usd()).isNotNull();
        assertThat(r.eur()).isNull();
    }

    @Test
    void parseXml_malformedXml_throws() {
        assertThatThrownBy(() -> service.parseXml("bu hic xml degil"));
    }

    @Test
    void getRates_networkFailure_returnsEmptyResponseWithoutThrowing() {
        // Gerçek bir ağ çağrısı olmadan (geçersiz bir host'a bağlanmaya çalışacak) hata yolunu
        // doğrular — getRates() hiçbir zaman exception fırlatmamalı.
        AppProperties props = new AppProperties();
        props.getExchangeRate().setUrl("http://127.0.0.1:1/does-not-exist");
        ExchangeRateService svc = new ExchangeRateService(RestClient.builder(), props);

        ExchangeRateResponse r = svc.getRates();

        assertThat(r.usd()).isNull();
        assertThat(r.eur()).isNull();
        assertThat(r.source()).isEqualTo("TCMB");
    }
}
