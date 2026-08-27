package com.ikibm.catalog.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/** OrderItem'dan SNAPSHOT olarak kopyalanır — ürün adı/kodu/fiyat/KDV oranı fatura kesildiği andaki
 * haliyle donar, ürün veya sipariş kalemi sonradan değişse/silinse bile fatura değişmez. */
@Entity
@Table(name = "invoice_items")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_code", nullable = false)
    private String productCode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    /** OrderItem'daki gibi: lineTotal'e zaten KDV dahil mi (fiyat listesinden geldiyse) yoksa
     * üzerine KDV eklenmesi mi gerekiyordu (standart/bayi fiyatından geldiyse). */
    @Column(name = "price_includes_vat", nullable = false)
    private Boolean priceIncludesVat = false;

    /** Fatura kesildiği andaki KDV oranı (%), snapshot — app.invoice.default-vat-rate ileride
     * değişse bile bu satırın oranı sabit kalır. */
    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate;

    /** Bu satırın KDV tutarı — priceIncludesVat=true ise lineTotal içinden ayrıştırılmış (embedded),
     * false ise lineTotal üzerine eklenecek tutar. */
    @Column(name = "vat_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal vatAmount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    public Boolean getPriceIncludesVat() { return priceIncludesVat; }
    public void setPriceIncludesVat(Boolean priceIncludesVat) { this.priceIncludesVat = priceIncludesVat; }
    public BigDecimal getVatRate() { return vatRate; }
    public void setVatRate(BigDecimal vatRate) { this.vatRate = vatRate; }
    public BigDecimal getVatAmount() { return vatAmount; }
    public void setVatAmount(BigDecimal vatAmount) { this.vatAmount = vatAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    /** Bu satırın KDV dahil tutarı (grand total'a katkısı) — inclusive satırlarda lineTotal'in
     * kendisi zaten KDV dahildir, exclusive satırlarda lineTotal + vatAmount. */
    @Transient
    public BigDecimal getGrandTotal() {
        return Boolean.TRUE.equals(priceIncludesVat) ? lineTotal : lineTotal.add(vatAmount);
    }
}
