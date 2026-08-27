package com.ikibm.catalog.service;

import com.ikibm.catalog.config.AppProperties;
import com.ikibm.catalog.dto.ExchangeRateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TCMB'nin resmi güncel kur XML feed'inden USD/EUR alış-satış değerlerini çeker.
 *
 * ÖNEMLİ — SALT GÖSTERİM (READ-ONLY / DISPLAY-ONLY): Bu servis SADECE header'daki "Güncel Kur"
 * rozeti içindir. Döndürdüğü değerler hiçbir ürün fiyatlandırmasında, sepet/sipariş toplamında,
 * cari hesap/cari hareketinde, faturada veya teklifte KULLANILMAMALIDIR. Bu servis bilerek sadece
 * ExchangeRateApiController tarafından kullanılır — başka hiçbir servise/controller'a enjekte
 * edilmemelidir.
 *
 * Veri en fazla {@code app.exchange-rate.cache-minutes} dakikada bir TCMB'den taze çekilir, aradaki
 * çağrılar bellek içi önbellekten döner (kalıcı depolama yok, veritabanına hiçbir şey yazılmaz).
 * Ağ/parse hatası durumunda asla exception fırlatmaz — eski önbellek varsa onu, yoksa boş bir
 * (usd=null, eur=null) yanıt döner; bu widget hiçbir koşulda sayfayı bozmamalı.
 */
@Service
public class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

    private final RestClient restClient;
    private final AppProperties.ExchangeRate config;
    private final AtomicReference<CachedRates> cache = new AtomicReference<>();

    public ExchangeRateService(RestClient.Builder restClientBuilder, AppProperties appProperties) {
        this.config = appProperties.getExchangeRate();
        this.restClient = restClientBuilder.baseUrl(config.getUrl()).build();
    }

    public ExchangeRateResponse getRates() {
        CachedRates cached = cache.get();
        if (cached != null && Duration.between(cached.fetchedAt(), Instant.now()).toMinutes() < config.getCacheMinutes()) {
            return cached.response();
        }
        try {
            ExchangeRateResponse fresh = parseXml(fetchFromTcmb());
            cache.set(new CachedRates(fresh, Instant.now()));
            return fresh;
        } catch (Exception e) {
            log.warn("TCMB güncel kur verisi alınamadı, önbellek kullanılacak (varsa)", e);
            return cached != null ? cached.response() : ExchangeRateResponse.empty();
        }
    }

    private String fetchFromTcmb() {
        // TCMB'nin sunucusu (WAF/CDN) gzip ile yanıt verdiğinde JDK'nın java.net.http.HttpClient
        // tabanlı otomatik gzip açma mekanizmasıyla "incorrect header check" hatası veriyor — sıkıştırma
        // isteği en baştan gönderilmeyerek (Accept-Encoding: identity) bu sorun tamamen bypass edilir.
        return restClient.get().header("Accept-Encoding", "identity").retrieve().body(String.class);
    }

    /** Saf ayrıştırma fonksiyonu (I/O yok) — doğrudan birim testle doğrulanır. Bozuk XML'de exception
     * fırlatır; onu yakalayıp güvenli bir fallback'e çevirmek getRates()'in sorumluluğudur. */
    ExchangeRateResponse parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // XXE sertleştirmesi — TCMB güvenilir bir kaynak olsa da dış girdi ayrıştıran her yerde standart pratik.
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        XPath xpath = XPathFactory.newInstance().newXPath();

        return new ExchangeRateResponse(extractCurrency(doc, xpath, "USD"), extractCurrency(doc, xpath, "EUR"), "TCMB");
    }

    private ExchangeRateResponse.CurrencyRate extractCurrency(Document doc, XPath xpath, String kod) {
        try {
            String buyingText = xpath.evaluate("/Tarih_Date/Currency[@Kod='" + kod + "']/ForexBuying", doc);
            String sellingText = xpath.evaluate("/Tarih_Date/Currency[@Kod='" + kod + "']/ForexSelling", doc);
            if (buyingText == null || buyingText.isBlank() || sellingText == null || sellingText.isBlank()) {
                return null;
            }
            return new ExchangeRateResponse.CurrencyRate(new BigDecimal(buyingText.trim()), new BigDecimal(sellingText.trim()));
        } catch (Exception e) {
            log.warn("TCMB feed'inde {} kuru ayrıştırılamadı", kod, e);
            return null;
        }
    }

    private record CachedRates(ExchangeRateResponse response, Instant fetchedAt) {
    }
}
