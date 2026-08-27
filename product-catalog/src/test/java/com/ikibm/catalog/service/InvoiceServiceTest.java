package com.ikibm.catalog.service;

import com.ikibm.catalog.config.AppProperties;
import com.ikibm.catalog.entity.CariReferenceType;
import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.entity.Invoice;
import com.ikibm.catalog.entity.InvoiceStatus;
import com.ikibm.catalog.entity.Order;
import com.ikibm.catalog.entity.OrderItem;
import com.ikibm.catalog.entity.OrderStatus;
import com.ikibm.catalog.entity.User;
import com.ikibm.catalog.exception.ConflictException;
import com.ikibm.catalog.exception.NotFoundException;
import com.ikibm.catalog.repository.InvoiceRepository;
import com.ikibm.catalog.repository.OrderRepository;
import com.ikibm.catalog.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Order → Invoice → CariTransaction(DEBT) akışının servis seviyesi testleri. CariAccountService mock'lanır
 * — "ödenmiş fatura iptal edilemez" gibi cari-içi kurallar CariAccountServiceTest'te gerçek mantıkla ayrıca
 * doğrulanır, burada sadece InvoiceService'in CariAccountService'i doğru çağırıp çağırmadığı test edilir. */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private CariAccountService cariAccountService;

    private AppProperties appProperties;
    private InvoiceService service;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getCompany().setName("2M Bilişim Ltd. Şti.");
        appProperties.getCompany().setTaxNumber("1234567890");
        appProperties.getCompany().setTaxOffice("Kadıköy Vergi Dairesi");
        appProperties.getCompany().setAddress("İstanbul");
        appProperties.getInvoice().setDefaultVatRate(new BigDecimal("20"));

        service = new InvoiceService(invoiceRepository, orderRepository, userRepository, cariAccountService, appProperties);

        lenient().when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) i.setId(42);
            return i;
        });
        lenient().when(userRepository.getReferenceById(any(Integer.class))).thenAnswer(inv -> {
            User u = new User();
            u.setId(inv.getArgument(0));
            return u;
        });
    }

    private User customer(Integer id) {
        User u = new User();
        u.setId(id);
        u.setCompanyName("ACME Ticaret Ltd.");
        u.setName("Ahmet");
        u.setSurname("Yılmaz");
        u.setTaxNumber("9876543210");
        u.setTaxOffice("Beşiktaş Vergi Dairesi");
        u.setBillingAddress("İstanbul, Türkiye");
        return u;
    }

    private OrderItem item(BigDecimal total, Currency currency, boolean includesVat) {
        OrderItem oi = new OrderItem();
        oi.setProductName("Ürün");
        oi.setProductCode("KOD1");
        oi.setQty(1);
        oi.setUnitPrice(total);
        oi.setTotalPrice(total);
        oi.setCurrency(currency);
        oi.setPriceIncludesVat(includesVat);
        return oi;
    }

    private Order order(Integer id, OrderStatus status, User user, OrderItem... items) {
        Order o = new Order();
        o.setId(id);
        o.setStatus(status);
        o.setUser(user);
        for (OrderItem it : items) {
            it.setOrder(o);
            o.getItems().add(it);
        }
        return o;
    }

    @Test
    void createFromOrder_confirmedOrder_succeeds() {
        Order o = order(10, OrderStatus.CONFIRMED, customer(4), item(new BigDecimal("1000.00"), Currency.TRY, false));
        when(orderRepository.findById(10)).thenReturn(Optional.of(o));

        Invoice invoice = service.createFromOrder(10, 1);

        assertThat(invoice.getId()).isEqualTo(42);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(invoice.getInvoiceNumber()).matches("FTR-\\d{4}-0042");
    }

    @Test
    void createFromOrder_cancelledOrder_isRejected() {
        Order o = order(11, OrderStatus.CANCELLED, customer(4), item(BigDecimal.TEN, Currency.TRY, false));
        when(orderRepository.findById(11)).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> service.createFromOrder(11, 1)).isInstanceOf(ConflictException.class);
        verify(cariAccountService, never()).recordInvoiceDebt(any(), any(), any(), any());
    }

    @Test
    void createFromOrder_alreadyInvoicedOrder_isRejected() {
        Order o = order(12, OrderStatus.CONFIRMED, customer(4), item(BigDecimal.TEN, Currency.TRY, false));
        when(orderRepository.findById(12)).thenReturn(Optional.of(o));
        when(invoiceRepository.existsByOrder_Id(12)).thenReturn(true);

        assertThatThrownBy(() -> service.createFromOrder(12, 1))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("zaten faturalandırılmış");
    }

    @Test
    void createFromOrder_recordsCariDebtInCorrectCurrencyAndAmount() {
        Order o = order(13, OrderStatus.CONFIRMED, customer(4), item(new BigDecimal("1000.00"), Currency.TRY, false));
        when(orderRepository.findById(13)).thenReturn(Optional.of(o));

        service.createFromOrder(13, 1);

        verify(cariAccountService).recordInvoiceDebt(eq(4), eq(42),
                eq(Map.of(Currency.TRY, new BigDecimal("1200.00"))), anyString());
    }

    @Test
    void createFromOrder_multiCurrency_groupsDebtPerCurrencySeparately() {
        Order o = order(14, OrderStatus.CONFIRMED, customer(4),
                item(new BigDecimal("100.00"), Currency.USD, false),
                item(new BigDecimal("2000.00"), Currency.TRY, true));
        when(orderRepository.findById(14)).thenReturn(Optional.of(o));

        service.createFromOrder(14, 1);

        verify(cariAccountService).recordInvoiceDebt(eq(4), eq(42),
                eq(Map.of(Currency.USD, new BigDecimal("120.00"), Currency.TRY, new BigDecimal("2000.00"))), anyString());
    }

    @Test
    void createFromOrder_existingOrderReferencedDebt_blocksInvoiceAndDoesNotDoubleDebt() {
        Order o = order(15, OrderStatus.CONFIRMED, customer(4), item(BigDecimal.TEN, Currency.TRY, false));
        when(orderRepository.findById(15)).thenReturn(Optional.of(o));
        when(cariAccountService.hasExistingDebtForReference(CariReferenceType.ORDER, 15)).thenReturn(true);

        assertThatThrownBy(() -> service.createFromOrder(15, 1))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("sipariş bazlı cari borç");
        verify(cariAccountService, never()).recordInvoiceDebt(any(), any(), any(), any());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createFromOrder_missingCustomerTaxInfo_isRejected() {
        User u = customer(4);
        u.setTaxNumber(null);
        Order o = order(16, OrderStatus.CONFIRMED, u, item(BigDecimal.TEN, Currency.TRY, false));
        when(orderRepository.findById(16)).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> service.createFromOrder(16, 1))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("vergi bilgileri eksik");
    }

    @Test
    void createFromOrder_missingSellerConfig_isRejected() {
        appProperties.getCompany().setTaxNumber("");
        Order o = order(17, OrderStatus.CONFIRMED, customer(4), item(BigDecimal.TEN, Currency.TRY, false));
        when(orderRepository.findById(17)).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> service.createFromOrder(17, 1))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Satıcı şirket bilgileri eksik");
    }

    @Test
    void createFromOrder_snapshotsSurviveLaterUserChanges() {
        User u = customer(4);
        Order o = order(18, OrderStatus.CONFIRMED, u, item(BigDecimal.TEN, Currency.TRY, false));
        when(orderRepository.findById(18)).thenReturn(Optional.of(o));

        Invoice invoice = service.createFromOrder(18, 1);

        u.setCompanyName("Değişen Firma Adı");
        u.setTaxNumber("0000000000");
        u.setBillingAddress("Yeni Adres");

        assertThat(invoice.getCustomerCompanyName()).isEqualTo("ACME Ticaret Ltd.");
        assertThat(invoice.getCustomerTaxNumber()).isEqualTo("9876543210");
        assertThat(invoice.getCustomerAddress()).isEqualTo("İstanbul, Türkiye");
    }

    @Test
    void getForUser_otherUsersInvoice_throwsNotFound() {
        when(invoiceRepository.findByIdAndUser_Id(5, 99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getForUser(99, 5)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void cancel_alreadyCancelled_isRejectedWithoutTouchingCari() {
        Invoice invoice = new Invoice();
        invoice.setId(7);
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setUser(customer(4));
        when(invoiceRepository.findById(7)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.cancel(7, 1)).isInstanceOf(ConflictException.class);
        verify(cariAccountService, never()).reverseInvoiceDebt(any(), any(), any(), any());
    }

    @Test
    void cancel_unpaidInvoice_succeedsAndReversesCariDebt() {
        Invoice invoice = new Invoice();
        invoice.setId(8);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setInvoiceNumber("FTR-2026-0008");
        invoice.setUser(customer(4));
        when(invoiceRepository.findById(8)).thenReturn(Optional.of(invoice));

        Invoice cancelled = service.cancel(8, 1);

        assertThat(cancelled.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
        assertThat(cancelled.getCancelledAt()).isNotNull();
        verify(cariAccountService).reverseInvoiceDebt(eq(4), eq(8), anyString(), eq(1));
    }

    @Test
    void cancel_paidInvoice_propagatesConflictAndLeavesInvoiceUnchanged() {
        Invoice invoice = new Invoice();
        invoice.setId(9);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setUser(customer(4));
        when(invoiceRepository.findById(9)).thenReturn(Optional.of(invoice));
        doThrow(new ConflictException("Bu faturaya ödeme işlendiği için fatura iptal edilemez."))
                .when(cariAccountService).reverseInvoiceDebt(eq(4), eq(9), anyString(), eq(1));

        assertThatThrownBy(() -> service.cancel(9, 1)).isInstanceOf(ConflictException.class);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(invoice.getCancelledAt()).isNull();
    }
}
