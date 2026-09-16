package com.ikibm.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Seed seed = new Seed();
    private Upload upload = new Upload();
    private Company company = new Company();
    private Invoice invoice = new Invoice();
    private ExchangeRate exchangeRate = new ExchangeRate();
    private Resend resend = new Resend();

    public Seed getSeed() { return seed; }
    public void setSeed(Seed seed) { this.seed = seed; }
    public Upload getUpload() { return upload; }
    public void setUpload(Upload upload) { this.upload = upload; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }
    public ExchangeRate getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(ExchangeRate exchangeRate) { this.exchangeRate = exchangeRate; }
    public Resend getResend() { return resend; }
    public void setResend(Resend resend) { this.resend = resend; }

    public static class Seed {
        private String superAdminEmail = "superadmin@2mbilisim.local";
        private String superAdminPassword = "ChangeMe123!";
        public String getSuperAdminEmail() { return superAdminEmail; }
        public void setSuperAdminEmail(String v) { this.superAdminEmail = v; }
        public String getSuperAdminPassword() { return superAdminPassword; }
        public void setSuperAdminPassword(String v) { this.superAdminPassword = v; }
    }

    /** Yerel disk görsel deposu kök klasörü. */
    public static class Upload {
        private String dir = "uploads";
        public String getDir() { return dir; }
        public void setDir(String dir) { this.dir = dir; }
    }

    /** Satıcı (bizim şirket) fatura bilgileri — her fatura oluşturulurken buradan okunup Invoice
     * üzerinde SNAPSHOT olarak dondurulur; bu ayarlar sonradan değişse bile geçmiş faturalar değişmez. */
    public static class Company {
        private String name = "";
        private String taxNumber = "";
        private String taxOffice = "";
        private String address = "";
        private String phone = "";
        private String email = "";
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getTaxNumber() { return taxNumber; }
        public void setTaxNumber(String taxNumber) { this.taxNumber = taxNumber; }
        public String getTaxOffice() { return taxOffice; }
        public void setTaxOffice(String taxOffice) { this.taxOffice = taxOffice; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    /** Fatura oluşturma ayarları — KDV oranı burada tek bir yerden okunur, InvoiceItem üzerinde
     * her satır için snapshot olarak (vatRate) donar; oran ileride değişirse eski faturalar etkilenmez. */
    public static class Invoice {
        private BigDecimal defaultVatRate = new BigDecimal("20");
        public BigDecimal getDefaultVatRate() { return defaultVatRate; }
        public void setDefaultVatRate(BigDecimal defaultVatRate) { this.defaultVatRate = defaultVatRate; }
    }

    /** TCMB güncel kur XML feed'i ayarları — SADECE header'daki görsel "Güncel Kur" rozeti için;
     * fiyatlandırma/sipariş/cari/fatura hesaplarında KULLANILMAZ. */
    public static class ExchangeRate {
        private String url = "https://www.tcmb.gov.tr/kurlar/today.xml";
        private int cacheMinutes = 60;
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public int getCacheMinutes() { return cacheMinutes; }
        public void setCacheMinutes(int cacheMinutes) { this.cacheMinutes = cacheMinutes; }
    }

    /** Resend (mail gönderme) ayarları — apiKey KESİNLİKLE bir varsayılan değer taşımaz, sadece
     * RESEND_API_KEY ortam değişkeninden okunur; boşsa MailService gönderimi reddeder. */
    public static class Resend {
        private String apiKey = "";
        private String from = "noreply@2mbilisim.net";
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
    }
}
