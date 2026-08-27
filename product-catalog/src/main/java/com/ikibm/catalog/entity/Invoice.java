package com.ikibm.catalog.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Bir siparişten kesilen fatura — muhasebe belgesi niteliğinde olduğu için satıcı/müşteri bilgileri
 * (isim/vergi no/vergi dairesi/adres) SNAPSHOT olarak tutulur: User veya app.company yapılandırması
 * sonradan değişse bile geçmişte kesilmiş bir fatura asla değişmez. Gerçek e-Fatura/e-Arşiv/GİB
 * numarası DEĞİLDİR — sadece dahili/görsel bir referans numarasıdır (bkz. invoiceNumber). */
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** "FTR-{yıl}-{id, 4 haneye sıfırla doldurulmuş}" — Invoice.id'den türetilir, resmi bir sayaçtan
     * DEĞİL. Sadece dahili/görsel referans; GİB/e-Fatura numarası değildir. */
    @Column(name = "invoice_number", nullable = false, unique = true, length = 30)
    private String invoiceNumber;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    /** Faturanın kesildiği müşteri — User silinemediği için (hiçbir yerde kullanıcı silme özelliği yok)
     * bu referans güvenle korunur, ama görüntülenecek bilgiler yine de aşağıdaki snapshot alanlarından okunur. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.ISSUED;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "seller_company_name", nullable = false)
    private String sellerCompanyName;

    @Column(name = "seller_tax_number", nullable = false, length = 50)
    private String sellerTaxNumber;

    @Column(name = "seller_tax_office", nullable = false, length = 150)
    private String sellerTaxOffice;

    @Column(name = "seller_address", nullable = false, length = 500)
    private String sellerAddress;

    @Column(name = "customer_company_name", nullable = false)
    private String customerCompanyName;

    @Column(name = "customer_tax_number", nullable = false, length = 50)
    private String customerTaxNumber;

    @Column(name = "customer_tax_office", nullable = false, length = 150)
    private String customerTaxOffice;

    @Column(name = "customer_address", nullable = false, length = 500)
    private String customerAddress;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<InvoiceItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant dueDate) { this.dueDate = dueDate; }
    public String getSellerCompanyName() { return sellerCompanyName; }
    public void setSellerCompanyName(String sellerCompanyName) { this.sellerCompanyName = sellerCompanyName; }
    public String getSellerTaxNumber() { return sellerTaxNumber; }
    public void setSellerTaxNumber(String sellerTaxNumber) { this.sellerTaxNumber = sellerTaxNumber; }
    public String getSellerTaxOffice() { return sellerTaxOffice; }
    public void setSellerTaxOffice(String sellerTaxOffice) { this.sellerTaxOffice = sellerTaxOffice; }
    public String getSellerAddress() { return sellerAddress; }
    public void setSellerAddress(String sellerAddress) { this.sellerAddress = sellerAddress; }
    public String getCustomerCompanyName() { return customerCompanyName; }
    public void setCustomerCompanyName(String customerCompanyName) { this.customerCompanyName = customerCompanyName; }
    public String getCustomerTaxNumber() { return customerTaxNumber; }
    public void setCustomerTaxNumber(String customerTaxNumber) { this.customerTaxNumber = customerTaxNumber; }
    public String getCustomerTaxOffice() { return customerTaxOffice; }
    public void setCustomerTaxOffice(String customerTaxOffice) { this.customerTaxOffice = customerTaxOffice; }
    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public User getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(User cancelledBy) { this.cancelledBy = cancelledBy; }
    public List<InvoiceItem> getItems() { return items; }
    public void setItems(List<InvoiceItem> items) { this.items = items; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
