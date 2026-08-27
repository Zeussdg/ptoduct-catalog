package com.ikibm.catalog.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cari_transactions")
public class CariTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cari_account_id", nullable = false)
    private CariAccount cariAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CariTransactionType type;

    /** Her zaman pozitif büyüklük beklenir (ADJUSTMENT hariç, o işaretli girilebilir);
     * bakiyeye etkisinin yönü type'a göre serviste belirlenir. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency = Currency.TRY;

    /** Bu hareketten hemen sonraki hesap bakiyesi — sadece currency == cariAccount.currency ise doldurulur. */
    @Column(name = "balance_after", precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    /** Bu borcun ne kadarı kapatıldı — ileride "tek ödeme birden fazla borcu kapatır" için hazırlık alanı. */
    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "transaction_date", nullable = false)
    private Instant transactionDate;

    @Column(name = "due_date")
    private Instant dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private CariPaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type")
    private CariReferenceType referenceType;

    @Column(name = "reference_id")
    private Integer referenceId;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public CariAccount getCariAccount() { return cariAccount; }
    public void setCariAccount(CariAccount cariAccount) { this.cariAccount = cariAccount; }
    public CariTransactionType getType() { return type; }
    public void setType(CariTransactionType type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public Instant getTransactionDate() { return transactionDate; }
    public void setTransactionDate(Instant transactionDate) { this.transactionDate = transactionDate; }
    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant dueDate) { this.dueDate = dueDate; }
    public CariPaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(CariPaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public CariReferenceType getReferenceType() { return referenceType; }
    public void setReferenceType(CariReferenceType referenceType) { this.referenceType = referenceType; }
    public Integer getReferenceId() { return referenceId; }
    public void setReferenceId(Integer referenceId) { this.referenceId = referenceId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
