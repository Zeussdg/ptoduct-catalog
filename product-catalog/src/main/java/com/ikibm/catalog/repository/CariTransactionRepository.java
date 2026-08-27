package com.ikibm.catalog.repository;

import com.ikibm.catalog.entity.CariReferenceType;
import com.ikibm.catalog.entity.CariTransaction;
import com.ikibm.catalog.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface CariTransactionRepository extends JpaRepository<CariTransaction, Integer> {

    /** Gerçek oluşturulma sırasına göre (en yeni üstte) — "Tarih" sütunundaki (elle girilebilen,
     * aynı gün için sıralaması belirsiz olan) transaction_date yerine id'ye göre sıralanır, çünkü
     * "Bakiye" sütunu (balance_after) tam olarak bu oluşturulma sırasına göre hesaplanmıştır. */
    List<CariTransaction> findByCariAccount_IdOrderByIdDesc(Integer cariAccountId);

    List<CariTransaction> findByCariAccount_IdAndCurrency(Integer cariAccountId, Currency currency);

    /** Fatura akışının "bu sipariş/fatura için zaten bir hareket var mı" kontrolleri için —
     * hem eski ORDER referanslı (artık yazılmıyor, sadece geçmiş kayıtlar) hem yeni INVOICE
     * referanslı kayıtları bulur. */
    boolean existsByReferenceTypeAndReferenceId(CariReferenceType referenceType, Integer referenceId);

    List<CariTransaction> findByReferenceTypeAndReferenceId(CariReferenceType referenceType, Integer referenceId);

    @Query("select coalesce(sum(t.amount),0) from CariTransaction t " +
            "where t.type = com.ikibm.catalog.entity.CariTransactionType.DEBT and t.currency = :currency")
    BigDecimal sumTotalDebt(@Param("currency") Currency currency);

    @Query("select coalesce(sum(t.amount),0) from CariTransaction t " +
            "where t.type = com.ikibm.catalog.entity.CariTransactionType.PAYMENT and t.currency = :currency")
    BigDecimal sumTotalCollected(@Param("currency") Currency currency);

    @Query("select coalesce(sum(t.amount),0) from CariTransaction t " +
            "where t.type = com.ikibm.catalog.entity.CariTransactionType.PAYMENT and t.currency = :currency " +
            "and t.transactionDate >= :from and t.transactionDate < :to")
    BigDecimal sumCollectedInRange(@Param("currency") Currency currency, @Param("from") Instant from, @Param("to") Instant to);

    @Query("select t from CariTransaction t where t.cariAccount.id = :accountId " +
            "and t.type = com.ikibm.catalog.entity.CariTransactionType.DEBT " +
            "and t.dueDate is not null and t.dueDate < :now and t.paidAmount < t.amount order by t.dueDate asc")
    List<CariTransaction> findOverdueByAccount(@Param("accountId") Integer accountId, @Param("now") Instant now);

    @Query("select t from CariTransaction t where t.type = com.ikibm.catalog.entity.CariTransactionType.DEBT " +
            "and t.dueDate is not null and t.dueDate < :now and t.paidAmount < t.amount and t.currency = :currency")
    List<CariTransaction> findAllOverdue(@Param("currency") Currency currency, @Param("now") Instant now);

    @Query("select t from CariTransaction t where t.cariAccount.id = :accountId " +
            "and t.type = com.ikibm.catalog.entity.CariTransactionType.DEBT " +
            "and t.dueDate is not null and t.dueDate >= :from and t.dueDate <= :to and t.paidAmount < t.amount order by t.dueDate asc")
    List<CariTransaction> findUpcomingDueByAccount(@Param("accountId") Integer accountId, @Param("from") Instant from, @Param("to") Instant to);
}
