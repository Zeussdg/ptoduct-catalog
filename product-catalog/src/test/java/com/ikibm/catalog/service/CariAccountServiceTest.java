package com.ikibm.catalog.service;

import com.ikibm.catalog.entity.CariAccount;
import com.ikibm.catalog.entity.CariDueStatus;
import com.ikibm.catalog.entity.CariPaymentMethod;
import com.ikibm.catalog.entity.CariReferenceType;
import com.ikibm.catalog.entity.CariTransaction;
import com.ikibm.catalog.entity.CariTransactionType;
import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.entity.User;
import com.ikibm.catalog.exception.ConflictException;
import com.ikibm.catalog.repository.CariAccountRepository;
import com.ikibm.catalog.repository.CariTransactionRepository;
import com.ikibm.catalog.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CariAccountServiceTest {

    @Mock private CariAccountRepository cariAccountRepository;
    @Mock private CariTransactionRepository cariTransactionRepository;
    @Mock private UserRepository userRepository;

    private CariAccountService service;

    /** Kaydedilmiş hareketleri, gerçek bir veritabanı yerine bellekte tutan sahte depo — outstandingBalance/
     * recalculateBalance hesaplarının findByCariAccount_IdAndCurrency çağrısına yanıt vermesi için gerekli. */
    private final List<CariTransaction> savedTransactions = new ArrayList<>();
    private int nextTxId = 1;

    @BeforeEach
    void setUp() {
        service = new CariAccountService(cariAccountRepository, cariTransactionRepository, userRepository);

        lenient().when(userRepository.getReferenceById(anyInt())).thenAnswer(inv -> {
            User u = new User();
            u.setId(inv.getArgument(0));
            return u;
        });

        lenient().when(cariAccountRepository.save(any(CariAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        lenient().when(cariTransactionRepository.save(any(CariTransaction.class))).thenAnswer(inv -> {
            CariTransaction t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(nextTxId++);
                savedTransactions.add(t);
            }
            return t;
        });

        lenient().when(cariTransactionRepository.findByCariAccount_IdAndCurrency(anyInt(), any(Currency.class)))
                .thenAnswer(inv -> savedTransactions.stream()
                        .filter(t -> t.getCariAccount().getId().equals(inv.getArgument(0)) && t.getCurrency() == inv.getArgument(1))
                        .toList());

        lenient().when(cariTransactionRepository.existsByReferenceTypeAndReferenceId(any(), anyInt()))
                .thenAnswer(inv -> savedTransactions.stream()
                        .anyMatch(t -> t.getReferenceType() == inv.getArgument(0) && inv.getArgument(1).equals(t.getReferenceId())));

        lenient().when(cariTransactionRepository.findByReferenceTypeAndReferenceId(any(), anyInt()))
                .thenAnswer(inv -> savedTransactions.stream()
                        .filter(t -> t.getReferenceType() == inv.getArgument(0) && inv.getArgument(1).equals(t.getReferenceId()))
                        .toList());
    }

    private CariAccount accountFor(Integer userId, Currency currency) {
        CariAccount account = new CariAccount();
        account.setId(userId);
        User user = new User();
        user.setId(userId);
        account.setUser(user);
        account.setCurrency(currency);
        account.setCreditLimit(BigDecimal.ZERO);
        account.setCurrentBalance(BigDecimal.ZERO);
        return account;
    }

    @Test
    void getOrCreateForUser_createsAccountWhenNoneExists() {
        when(cariAccountRepository.findByUser_Id(5)).thenReturn(Optional.empty());

        CariAccount created = service.getOrCreateForUser(5);

        assertThat(created.getCurrency()).isEqualTo(Currency.USD);
        assertThat(created.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getOrCreateForUser_returnsExistingAccountWithoutDuplicating() {
        CariAccount existing = accountFor(7, Currency.TRY);
        when(cariAccountRepository.findByUser_Id(7)).thenReturn(Optional.of(existing));

        CariAccount result = service.getOrCreateForUser(7);

        assertThat(result).isSameAs(existing);
    }

    @Test
    void addManualTransaction_debt_increasesBalance() {
        CariAccount account = accountFor(1, Currency.TRY);
        when(cariAccountRepository.findByUser_Id(1)).thenReturn(Optional.of(account));

        CariTransaction tx = service.addManualTransaction(1, CariTransactionType.DEBT, new BigDecimal("50000"),
                Currency.TRY, Instant.now(), null, "Sipariş #1052", null);

        assertThat(tx.getBalanceAfter()).isEqualByComparingTo("50000");
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("50000");
    }

    @Test
    void addManualTransaction_adjustment_negativeAmount_decreasesBalance() {
        CariAccount account = accountFor(1, Currency.TRY);
        when(cariAccountRepository.findByUser_Id(1)).thenReturn(Optional.of(account));

        service.addManualTransaction(1, CariTransactionType.DEBT, new BigDecimal("1000"), Currency.TRY, Instant.now(), null, null, null);
        CariTransaction adj = service.addManualTransaction(1, CariTransactionType.ADJUSTMENT, new BigDecimal("-300"),
                Currency.TRY, Instant.now(), null, "Borç silindi", null);

        assertThat(adj.getBalanceAfter()).isEqualByComparingTo("700");
    }

    @Test
    void addManualTransaction_rejectsPaymentOrRefundType() {
        assertThatThrownBy(() -> service.addManualTransaction(1, CariTransactionType.PAYMENT, BigDecimal.TEN,
                Currency.TRY, Instant.now(), null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recordPayment_reducesBalanceAndCreatesPaymentTransaction() {
        CariAccount account = accountFor(1, Currency.TRY);
        when(cariAccountRepository.findByUser_Id(1)).thenReturn(Optional.of(account));

        service.addManualTransaction(1, CariTransactionType.DEBT, new BigDecimal("50000"), Currency.TRY, Instant.now(), null, null, null);
        CariTransaction payment = service.recordPayment(1, new BigDecimal("20000"), Currency.TRY, Instant.now(),
                CariPaymentMethod.HAVALE, "Kısmi ödeme", null);

        assertThat(payment.getType()).isEqualTo(CariTransactionType.PAYMENT);
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("30000");
    }

    @Test
    void recordPayment_rejectsAmountExceedingOutstandingBalance() {
        CariAccount account = accountFor(1, Currency.TRY);
        when(cariAccountRepository.findByUser_Id(1)).thenReturn(Optional.of(account));

        service.addManualTransaction(1, CariTransactionType.DEBT, new BigDecimal("1000"), Currency.TRY, Instant.now(), null, null, null);

        assertThatThrownBy(() -> service.recordPayment(1, new BigDecimal("5000"), Currency.TRY, Instant.now(),
                CariPaymentMethod.NAKIT, null, null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void recordPayment_validatesPerCurrencyOutstandingBalance_notAccountPrimaryBalance() {
        CariAccount account = accountFor(1, Currency.TRY);
        when(cariAccountRepository.findByUser_Id(1)).thenReturn(Optional.of(account));

        service.addManualTransaction(1, CariTransactionType.DEBT, new BigDecimal("50000"), Currency.TRY, Instant.now(), null, null, null);

        assertThatThrownBy(() -> service.recordPayment(1, new BigDecimal("10"), Currency.USD, Instant.now(),
                CariPaymentMethod.NAKIT, null, null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void availableLimit_computesCreditLimitMinusCurrentBalance() {
        CariAccount account = accountFor(9, Currency.TRY);
        account.setCreditLimit(new BigDecimal("200000"));
        account.setCurrentBalance(new BigDecimal("125000"));
        when(cariAccountRepository.findById(9)).thenReturn(Optional.of(account));

        assertThat(service.availableLimit(9)).isEqualByComparingTo("75000");
    }

    @Test
    void dueStatus_returnsVadesiGectiForPastUnpaidDebt() {
        CariTransaction t = new CariTransaction();
        t.setType(CariTransactionType.DEBT);
        t.setAmount(new BigDecimal("1000"));
        t.setPaidAmount(BigDecimal.ZERO);
        t.setDueDate(Instant.now().minus(5, ChronoUnit.DAYS));

        assertThat(service.dueStatus(t)).isEqualTo(CariDueStatus.VADESI_GECTI);
    }

    @Test
    void dueStatus_returnsOdendiWhenPaidAmountEqualsAmount() {
        CariTransaction t = new CariTransaction();
        t.setType(CariTransactionType.DEBT);
        t.setAmount(new BigDecimal("1000"));
        t.setPaidAmount(new BigDecimal("1000"));

        assertThat(service.dueStatus(t)).isEqualTo(CariDueStatus.ODENDI);
    }

    @Test
    void dueStatus_returnsKismenOdendiWhenPartiallyPaid() {
        CariTransaction t = new CariTransaction();
        t.setType(CariTransactionType.DEBT);
        t.setAmount(new BigDecimal("1000"));
        t.setPaidAmount(new BigDecimal("400"));

        assertThat(service.dueStatus(t)).isEqualTo(CariDueStatus.KISMEN_ODENDI);
    }

    @Test
    void dueStatus_returnsBekliyorWhenNoDueDate() {
        CariTransaction t = new CariTransaction();
        t.setType(CariTransactionType.DEBT);
        t.setAmount(new BigDecimal("1000"));
        t.setPaidAmount(BigDecimal.ZERO);
        t.setDueDate(null);

        assertThat(service.dueStatus(t)).isEqualTo(CariDueStatus.BEKLIYOR);
    }

    @Test
    void overdueTotal_sumsOnlyPastDueUnpaidDebts() {
        CariAccount account = accountFor(1, Currency.TRY);
        when(cariAccountRepository.findById(1)).thenReturn(Optional.of(account));

        CariTransaction overdue = new CariTransaction();
        overdue.setCurrency(Currency.TRY);
        overdue.setAmount(new BigDecimal("1000"));
        overdue.setPaidAmount(new BigDecimal("300"));
        when(cariTransactionRepository.findOverdueByAccount(anyInt(), any(Instant.class))).thenReturn(List.of(overdue));

        assertThat(service.overdueTotal(1)).isEqualByComparingTo("700");
    }

    @Test
    void recordInvoiceDebt_createsDebtWithInvoiceReference() {
        CariAccount account = accountFor(1, Currency.TRY);
        when(cariAccountRepository.findByUser_Id(1)).thenReturn(Optional.of(account));

        service.recordInvoiceDebt(1, 55, java.util.Map.of(Currency.TRY, new BigDecimal("1200.00")), "Fatura FTR-2026-0055");

        assertThat(account.getCurrentBalance()).isEqualByComparingTo("1200.00");
        assertThat(savedTransactions).hasSize(1);
        CariTransaction t = savedTransactions.get(0);
        assertThat(t.getReferenceType()).isEqualTo(CariReferenceType.INVOICE);
        assertThat(t.getReferenceId()).isEqualTo(55);
        assertThat(t.getType()).isEqualTo(CariTransactionType.DEBT);
    }

    @Test
    void hasExistingDebtForReference_detectsPreviousOrderReferencedDebt() {
        CariAccount account = accountFor(1, Currency.TRY);
        when(cariAccountRepository.findByUser_Id(1)).thenReturn(Optional.of(account));
        service.addManualTransaction(1, CariTransactionType.DEBT, BigDecimal.TEN, Currency.TRY, Instant.now(), null, null, null);
        savedTransactions.get(0).setReferenceType(com.ikibm.catalog.entity.CariReferenceType.ORDER);
        savedTransactions.get(0).setReferenceId(77);

        assertThat(service.hasExistingDebtForReference(CariReferenceType.ORDER, 77)).isTrue();
        assertThat(service.hasExistingDebtForReference(CariReferenceType.ORDER, 78)).isFalse();
    }

    @Test
    void reverseInvoiceDebt_unpaidDebt_writesNegativeAdjustmentAndReducesBalance() {
        CariAccount account = accountFor(1, Currency.TRY);
        when(cariAccountRepository.findByUser_Id(1)).thenReturn(Optional.of(account));

        service.recordInvoiceDebt(1, 60, java.util.Map.of(Currency.TRY, new BigDecimal("1200.00")), "Fatura FTR-2026-0060");
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("1200.00");

        service.reverseInvoiceDebt(1, 60, "Fatura FTR-2026-0060 iptali", 9);

        assertThat(account.getCurrentBalance()).isEqualByComparingTo("0.00");
        CariTransaction reversal = savedTransactions.get(savedTransactions.size() - 1);
        assertThat(reversal.getType()).isEqualTo(CariTransactionType.ADJUSTMENT);
        assertThat(reversal.getAmount()).isEqualByComparingTo("-1200.00");
        assertThat(reversal.getReferenceType()).isEqualTo(CariReferenceType.INVOICE);
    }

    @Test
    void reverseInvoiceDebt_multiCurrency_reversesEachCurrencySeparately() {
        CariAccount account = accountFor(1, Currency.TRY);
        when(cariAccountRepository.findByUser_Id(1)).thenReturn(Optional.of(account));

        service.recordInvoiceDebt(1, 61,
                java.util.Map.of(Currency.TRY, new BigDecimal("1000.00"), Currency.USD, new BigDecimal("50.00")),
                "Fatura FTR-2026-0061");

        service.reverseInvoiceDebt(1, 61, "Fatura FTR-2026-0061 iptali", 9);

        long reversalCount = savedTransactions.stream()
                .filter(t -> t.getType() == CariTransactionType.ADJUSTMENT && t.getReferenceId().equals(61))
                .count();
        assertThat(reversalCount).isEqualTo(2);
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void reverseInvoiceDebt_paidDebt_throwsConflictAndWritesNoAdjustment() {
        CariAccount account = accountFor(1, Currency.TRY);
        when(cariAccountRepository.findByUser_Id(1)).thenReturn(Optional.of(account));

        service.recordInvoiceDebt(1, 62, java.util.Map.of(Currency.TRY, new BigDecimal("1200.00")), "Fatura FTR-2026-0062");
        savedTransactions.get(0).setPaidAmount(new BigDecimal("500.00"));
        int countBefore = savedTransactions.size();

        assertThatThrownBy(() -> service.reverseInvoiceDebt(1, 62, "iptal", 9))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ödeme işlendiği");
        assertThat(savedTransactions).hasSize(countBefore);
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("1200.00");
    }

    @Test
    void reverseInvoiceDebt_noDebtsForInvoice_doesNothing() {
        // invoiceId=999 için hiç DEBT hareketi yok — getOrCreateForUser'a hiç gidilmemeli (erken dönüş),
        // bu yüzden cariAccountRepository burada kasıtlı olarak stub'lanmıyor.
        service.reverseInvoiceDebt(1, 999, "iptal", 9);

        assertThat(savedTransactions).isEmpty();
    }
}
