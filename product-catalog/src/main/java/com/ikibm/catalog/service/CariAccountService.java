package com.ikibm.catalog.service;

import com.ikibm.catalog.entity.CariAccount;
import com.ikibm.catalog.entity.CariDueStatus;
import com.ikibm.catalog.entity.CariPaymentMethod;
import com.ikibm.catalog.entity.CariReferenceType;
import com.ikibm.catalog.entity.CariTransaction;
import com.ikibm.catalog.entity.CariTransactionType;
import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.exception.ConflictException;
import com.ikibm.catalog.exception.NotFoundException;
import com.ikibm.catalog.repository.CariAccountRepository;
import com.ikibm.catalog.repository.CariTransactionRepository;
import com.ikibm.catalog.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Cari hesap (borç/ödeme/bakiye/vade) için tüm finansal hesaplamaların merkezi olduğu servis.
 * PriceList/Veresiye fiyatlandırma sistemiyle hiçbir ilişkisi yoktur — tamamen bağımsız çalışır. */
@Service
public class CariAccountService {

    private final CariAccountRepository cariAccountRepository;
    private final CariTransactionRepository cariTransactionRepository;
    private final UserRepository userRepository;

    public CariAccountService(CariAccountRepository cariAccountRepository,
                              CariTransactionRepository cariTransactionRepository,
                              UserRepository userRepository) {
        this.cariAccountRepository = cariAccountRepository;
        this.cariTransactionRepository = cariTransactionRepository;
        this.userRepository = userRepository;
    }

    /** Var olan hesabı döner, yoksa (admin ilk kez cari sekmesine girdiğinde) TRY / limit 0 ile oluşturur. */
    @Transactional
    public CariAccount getOrCreateForUser(Integer userId) {
        return cariAccountRepository.findByUser_Id(userId).orElseGet(() -> {
            CariAccount account = new CariAccount();
            account.setUser(userRepository.getReferenceById(userId));
            account.setCurrency(Currency.USD);
            account.setCreditLimit(BigDecimal.ZERO);
            account.setCurrentBalance(BigDecimal.ZERO);
            return cariAccountRepository.save(account);
        });
    }

    /** Salt-okunur; hesap yoksa NotFoundException (müşteri tarafı kendine hesap oluşturamaz). */
    public CariAccount getForUser(Integer userId) {
        return cariAccountRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Cari hesap bulunamadı"));
    }

    public CariAccount get(Integer id) {
        return cariAccountRepository.findById(id).orElseThrow(() -> new NotFoundException("Cari hesap bulunamadı"));
    }

    public List<CariTransaction> history(Integer cariAccountId) {
        return cariTransactionRepository.findByCariAccount_IdOrderByIdDesc(cariAccountId);
    }

    /** Borç Ekle / Borç Düzeltmesi — DEBT veya ADJUSTMENT. PAYMENT/REFUND için recordPayment kullanılmalı. */
    @Transactional
    public CariTransaction addManualTransaction(Integer userId, CariTransactionType type, BigDecimal amount,
            Currency currency, Instant transactionDate, Instant dueDate, String description, Integer createdByUserId) {
        if (type == CariTransactionType.PAYMENT || type == CariTransactionType.REFUND) {
            throw new IllegalArgumentException("Ödeme/İade işlemleri için 'Ödeme Al' kullanılmalı");
        }
        if (amount == null || amount.signum() == 0) {
            throw new IllegalArgumentException("Tutar sıfır olamaz");
        }
        if (type == CariTransactionType.DEBT && amount.signum() < 0) {
            throw new IllegalArgumentException("Borç tutarı negatif olamaz");
        }
        CariAccount account = getOrCreateForUser(userId);
        return writeAndRecalculate(account, type, amount, currency, transactionDate, dueDate, null,
                CariReferenceType.MANUAL, null, description, createdByUserId);
    }

    /** Ödeme Al / Tahsilat Ekle — TEK ortak kod yolu; "borçtan fazla olamaz" kuralı burada merkezi. */
    @Transactional
    public CariTransaction recordPayment(Integer userId, BigDecimal amount, Currency currency, Instant paymentDate,
            CariPaymentMethod method, String description, Integer createdByUserId) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Ödeme tutarı sıfırdan büyük olmalı");
        }
        CariAccount account = getOrCreateForUser(userId);
        BigDecimal outstanding = outstandingBalance(account.getId(), currency);
        if (amount.compareTo(outstanding) > 0) {
            throw new ConflictException("Ödeme tutarı (" + amount + " " + currency
                    + ") mevcut borçtan (" + outstanding + " " + currency + ") fazla olamaz");
        }
        return writeAndRecalculate(account, CariTransactionType.PAYMENT, amount, currency, paymentDate, null,
                method, CariReferenceType.MANUAL, null, description, createdByUserId);
    }

    /** Bir faturadan (para birimi başına) otomatik borç yazar — cari borç artık SADECE bu metot
     * üzerinden, admin "Faturalandır" dediğinde oluşur (sipariş oluşturulduğunda değil). Eskiden
     * OrderService.createFromQuote içinde sipariş oluşur oluşmaz "Veresiye" kalemler için otomatik
     * borç yazan ayrı bir recordOrderDebt(...) metodu vardı; onaylanmadan/faturalandırılmadan cariye
     * yansımaması gerektiği için o mekanizma kaldırıldı. CariReferenceType.ORDER enum değeri ve o
     * dönemden kalma geçmiş CariTransaction kayıtları (referenceType=ORDER) hâlâ DB'de duruyor ve
     * hiç değiştirilmedi/silinmedi — sadece yeni borç yazımı artık bu yoldan geçmiyor. */
    @Transactional
    public void recordInvoiceDebt(Integer userId, Integer invoiceId, Map<Currency, BigDecimal> amountsByCurrency, String description) {
        if (amountsByCurrency == null || amountsByCurrency.isEmpty()) return;
        CariAccount account = getOrCreateForUser(userId);
        for (Map.Entry<Currency, BigDecimal> entry : amountsByCurrency.entrySet()) {
            if (entry.getValue() == null || entry.getValue().signum() <= 0) continue;
            writeAndRecalculate(account, CariTransactionType.DEBT, entry.getValue(), entry.getKey(), Instant.now(),
                    null, null, CariReferenceType.INVOICE, invoiceId, description, null);
        }
    }

    /** Bir sipariş veya fatura için referenceType/referenceId ile işaretlenmiş herhangi bir hareket
     * var mı — fatura oluşturma akışının "bu sipariş için zaten eski usul ORDER borcu var mı" ve
     * "bu fatura için zaten borç var mı" kontrollerinde kullanılır. */
    public boolean hasExistingDebtForReference(CariReferenceType referenceType, Integer referenceId) {
        return cariTransactionRepository.existsByReferenceTypeAndReferenceId(referenceType, referenceId);
    }

    /** Fatura iptalinde ilgili borcu ters çevirir (ADJUSTMENT, negatif tutar) — DEBT satırı SİLİNMEZ,
     * mevcut audit-trail felsefesiyle (hiçbir yerde hard-delete/update-in-place yok) tutarlı şekilde
     * yeni bir ters kayıt eklenir. Fatura kısmen/tamamen ödenmişse (paidAmount > 0) iptal engellenir —
     * bu projede ödeme-fatura eşleştirme (allocation) sistemi kurulmadığı için paidAmount bugün hiçbir
     * ödeme akışı tarafından otomatik güncellenmiyor; bu kontrol o alan ileride devreye alındığında
     * ek kod değişikliği gerektirmeden çalışacak şekilde şimdiden buraya eklendi. */
    @Transactional
    public void reverseInvoiceDebt(Integer userId, Integer invoiceId, String description, Integer actingUserId) {
        List<CariTransaction> invoiceTransactions = cariTransactionRepository
                .findByReferenceTypeAndReferenceId(CariReferenceType.INVOICE, invoiceId);
        List<CariTransaction> debts = invoiceTransactions.stream()
                .filter(t -> t.getType() == CariTransactionType.DEBT)
                .toList();
        for (CariTransaction debt : debts) {
            if (debt.getPaidAmount() != null && debt.getPaidAmount().signum() > 0) {
                throw new ConflictException("Bu faturaya ödeme işlendiği için fatura iptal edilemez.");
            }
        }
        if (debts.isEmpty()) return;
        CariAccount account = getOrCreateForUser(userId);
        for (CariTransaction debt : debts) {
            writeAndRecalculate(account, CariTransactionType.ADJUSTMENT, debt.getAmount().negate(), debt.getCurrency(),
                    Instant.now(), null, null, CariReferenceType.INVOICE, invoiceId, description, actingUserId);
        }
    }

    /** Belirli bir para biriminde kalan borç: sum(DEBT+ADJUSTMENT) - sum(PAYMENT+REFUND), o currency için. */
    private BigDecimal outstandingBalance(Integer accountId, Currency currency) {
        List<CariTransaction> sameCurrency = cariTransactionRepository.findByCariAccount_IdAndCurrency(accountId, currency);
        BigDecimal balance = BigDecimal.ZERO;
        for (CariTransaction t : sameCurrency) {
            balance = balance.add(signedDelta(t));
        }
        return balance;
    }

    private CariTransaction writeAndRecalculate(CariAccount account, CariTransactionType type, BigDecimal amount,
            Currency currency, Instant txDate, Instant dueDate, CariPaymentMethod method,
            CariReferenceType refType, Integer refId, String description, Integer createdByUserId) {
        CariTransaction t = new CariTransaction();
        t.setCariAccount(account);
        t.setType(type);
        t.setAmount(amount);
        t.setCurrency(currency);
        t.setTransactionDate(txDate != null ? txDate : Instant.now());
        t.setDueDate(dueDate);
        t.setPaymentMethod(method);
        t.setReferenceType(refType);
        t.setReferenceId(refId);
        t.setDescription(description);
        if (createdByUserId != null) {
            t.setCreatedBy(userRepository.getReferenceById(createdByUserId));
        }
        t = cariTransactionRepository.save(t);
        recalculateBalance(account, t);
        return t;
    }

    /** current_balance'ı her yazmadan sonra aynı para birimindeki tüm hareketlerden sıfırdan yeniden hesaplar
     * — böylece bakiye asla CariTransaction geçmişinden sapamaz. */
    private void recalculateBalance(CariAccount account, CariTransaction justWritten) {
        BigDecimal balance = outstandingBalance(account.getId(), account.getCurrency());
        account.setCurrentBalance(balance);
        cariAccountRepository.save(account);
        if (justWritten.getCurrency() == account.getCurrency()) {
            justWritten.setBalanceAfter(balance);
            cariTransactionRepository.save(justWritten);
        }
    }

    private BigDecimal signedDelta(CariTransaction t) {
        return switch (t.getType()) {
            case DEBT, ADJUSTMENT -> t.getAmount();
            case PAYMENT, REFUND -> t.getAmount().negate();
        };
    }

    public BigDecimal availableLimit(Integer cariAccountId) {
        CariAccount a = get(cariAccountId);
        return a.getCreditLimit().subtract(a.getCurrentBalance());
    }

    /** Hesabın birincil para birimi dışındaki hareketleri de görünür kılar — kur çevrimi YOK. */
    public Map<Currency, BigDecimal> balanceBreakdownByCurrency(Integer cariAccountId) {
        Map<Currency, BigDecimal> result = new EnumMap<>(Currency.class);
        for (CariTransaction t : cariTransactionRepository.findByCariAccount_IdOrderByIdDesc(cariAccountId)) {
            result.merge(t.getCurrency(), signedDelta(t), BigDecimal::add);
        }
        return result;
    }

    public CariDueStatus dueStatus(CariTransaction debt) {
        if (debt.getType() != CariTransactionType.DEBT) return null;
        if (debt.getPaidAmount().compareTo(debt.getAmount()) >= 0) return CariDueStatus.ODENDI;
        if (debt.getPaidAmount().signum() > 0) return CariDueStatus.KISMEN_ODENDI;
        if (debt.getDueDate() == null) return CariDueStatus.BEKLIYOR;
        Instant now = Instant.now();
        if (debt.getDueDate().isBefore(now)) return CariDueStatus.VADESI_GECTI;
        if (debt.getDueDate().truncatedTo(ChronoUnit.DAYS).equals(now.truncatedTo(ChronoUnit.DAYS))) {
            return CariDueStatus.VADESI_GELDI;
        }
        return CariDueStatus.BEKLIYOR;
    }

    /** Vade rozeti için CSS modifier sınıfı (başında boşlukla) — Thymeleaf'te iç içe ternary
     * ifade ayrıştırma sorunlarından kaçınmak için burada, template'te değil. */
    public String dueStatusBadgeClass(CariDueStatus status) {
        if (status == null) return "";
        return switch (status) {
            case VADESI_GECTI -> " adp__badge--danger";
            case ODENDI -> " adp__badge--success";
            case KISMEN_ODENDI -> " adp__badge--muted";
            case BEKLIYOR, VADESI_GELDI -> "";
        };
    }

    public BigDecimal overdueTotal(Integer cariAccountId) {
        CariAccount account = get(cariAccountId);
        BigDecimal total = BigDecimal.ZERO;
        for (CariTransaction t : cariTransactionRepository.findOverdueByAccount(cariAccountId, Instant.now())) {
            if (t.getCurrency() == account.getCurrency()) {
                total = total.add(t.getAmount().subtract(t.getPaidAmount()));
            }
        }
        return total;
    }

    public BigDecimal upcomingDueTotal(Integer cariAccountId, int days) {
        CariAccount account = get(cariAccountId);
        Instant now = Instant.now();
        BigDecimal total = BigDecimal.ZERO;
        for (CariTransaction t : cariTransactionRepository.findUpcomingDueByAccount(cariAccountId, now, now.plus(days, ChronoUnit.DAYS))) {
            if (t.getCurrency() == account.getCurrency()) {
                total = total.add(t.getAmount().subtract(t.getPaidAmount()));
            }
        }
        return total;
    }

    // ---- Dashboard hazırlığı (spec #16) — servis metodu olarak hazır, henüz hiçbir yere bağlanmıyor ----

    public BigDecimal totalDebt(Currency currency) {
        return cariTransactionRepository.sumTotalDebt(currency);
    }

    public BigDecimal totalCollected(Currency currency) {
        return cariTransactionRepository.sumTotalCollected(currency);
    }

    public BigDecimal overdueTotalAll(Currency currency) {
        BigDecimal total = BigDecimal.ZERO;
        for (CariTransaction t : cariTransactionRepository.findAllOverdue(currency, Instant.now())) {
            total = total.add(t.getAmount().subtract(t.getPaidAmount()));
        }
        return total;
    }

    public BigDecimal collectedThisMonth(Currency currency) {
        java.time.ZoneId zone = java.time.ZoneId.systemDefault();
        java.time.LocalDate today = java.time.LocalDate.now(zone);
        Instant monthStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant();
        Instant monthEnd = today.withDayOfMonth(1).plusMonths(1).atStartOfDay(zone).toInstant();
        return cariTransactionRepository.sumCollectedInRange(currency, monthStart, monthEnd);
    }

    public List<CariAccount> topDebtors(int limit) {
        return cariAccountRepository.findTop10ByOrderByCurrentBalanceDesc();
    }

    public List<CariAccount> overLimitAccounts() {
        return cariAccountRepository.findOverLimit();
    }
}
