package com.ikibm.catalog.service;

import com.ikibm.catalog.entity.Invoice;
import com.ikibm.catalog.entity.InvoiceStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Admin fatura listesi arama/filtre kriterleri — ProductSpecifications ile aynı desen. */
public final class InvoiceSpecifications {

    private InvoiceSpecifications() {
    }

    /** Fatura no, müşteri adı (snapshot) veya sipariş no (girilen metin sayısalsa) ile arar. */
    public static Specification<Invoice> search(String q) {
        if (q == null || q.isBlank()) return null;
        String like = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            List<Predicate> ors = new ArrayList<>();
            ors.add(cb.like(cb.lower(root.get("invoiceNumber")), like));
            ors.add(cb.like(cb.lower(root.get("customerCompanyName")), like));
            try {
                Integer orderId = Integer.valueOf(q.trim());
                ors.add(cb.equal(root.get("order").get("id"), orderId));
            } catch (NumberFormatException ignored) {
                // Girilen metin sayısal değilse sipariş no eşleşmesi denenmez.
            }
            return cb.or(ors.toArray(new Predicate[0]));
        };
    }

    public static Specification<Invoice> status(InvoiceStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Invoice> issuedFrom(Instant from) {
        if (from == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("issuedAt"), from);
    }

    /** Üst sınır HARİÇ (exclusive) — çağıran taraf "bitiş günü sonu" için bir sonraki günün başlangıcını geçmeli. */
    public static Specification<Invoice> issuedBefore(Instant beforeExclusive) {
        if (beforeExclusive == null) return null;
        return (root, query, cb) -> cb.lessThan(root.get("issuedAt"), beforeExclusive);
    }
}
