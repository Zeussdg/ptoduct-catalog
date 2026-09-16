package com.ikibm.catalog.service;

import com.ikibm.catalog.config.AppProperties;
import com.ikibm.catalog.entity.CariReferenceType;
import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.entity.Invoice;
import com.ikibm.catalog.entity.InvoiceItem;
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
import com.ikibm.catalog.util.InvoiceCalculator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Order → Invoice → CariTransaction(DEBT) akışının merkezi servisi. Cari borç artık SADECE bu servis
 * üzerinden (admin "Faturalandır" dediğinde) oluşur — OrderService'in sipariş oluşturulur oluşmaz
 * otomatik borç yazan eski recordCreditDebtIfAny/recordOrderDebt mekanizması kaldırıldı.
 */
@Service
public class InvoiceService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** CANCELLED sipariş kesinlikle bu kümede değildir — faturalandırma her zaman reddedilir. */
    private static final Set<OrderStatus> INVOICEABLE_STATUSES =
            EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CariAccountService cariAccountService;
    private final AppProperties appProperties;

    public InvoiceService(InvoiceRepository invoiceRepository, OrderRepository orderRepository,
                          UserRepository userRepository, CariAccountService cariAccountService,
                          AppProperties appProperties) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.cariAccountService = cariAccountService;
        this.appProperties = appProperties;
    }

    public List<Invoice> listForUser(Integer userId) {
        return invoiceRepository.findByUser_IdOrderByIssuedAtDesc(userId);
    }

    /** Müşteri tarafı erişimi: findByIdAndUser_Id sayesinde başka müşterinin faturası varsa NotFoundException
     * fırlatılır (403 yerine 404 — kaynağın varlığını bile sızdırmaz, mevcut OrderService.getForUser ile aynı desen). */
    public Invoice getForUser(Integer userId, Integer id) {
        return invoiceRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new NotFoundException("Fatura bulunamadı"));
    }

    public Invoice getByIdAdmin(Integer id) {
        return invoiceRepository.findById(id).orElseThrow(() -> new NotFoundException("Fatura bulunamadı"));
    }

    public Optional<Invoice> findByOrderId(Integer orderId) {
        return invoiceRepository.findByOrder_Id(orderId);
    }

    /** Admin fatura listesi — fatura no/müşteri adı/sipariş no araması, durum ve tarih aralığı filtresi
     * (ProductService.adminList ile aynı Specification+Page deseni), 20 kayıt/sayfa, en yeni önce. */
    public Page<Invoice> listAllAdmin(String q, InvoiceStatus status, Instant issuedFrom, Instant issuedBeforeExclusive, int page) {
        Specification<Invoice> spec = combine(
                InvoiceSpecifications.search(q),
                InvoiceSpecifications.status(status),
                InvoiceSpecifications.issuedFrom(issuedFrom),
                InvoiceSpecifications.issuedBefore(issuedBeforeExclusive));
        PageRequest pr = PageRequest.of(Math.max(0, page - 1), 20, Sort.by(Sort.Direction.DESC, "issuedAt"));
        return spec != null ? invoiceRepository.findAll(spec, pr) : invoiceRepository.findAll(pr);
    }

    /** Spring Data JPA 4.x'te Specification.and(null)/where(null) exception fırlattığı için (bkz.
     * ProductService.adminList'teki aynı not), null olan filtreler burada elenerek birleştirilir. */
    @SafeVarargs
    private Specification<Invoice> combine(Specification<Invoice>... specs) {
        Specification<Invoice> result = null;
        for (Specification<Invoice> s : specs) {
            if (s == null) continue;
            result = (result == null) ? Specification.where(s) : result.and(s);
        }
        return result;
    }

    @Transactional
    public Invoice createFromOrder(Integer orderId, Integer actingUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Sipariş bulunamadı"));

        if (!INVOICEABLE_STATUSES.contains(order.getStatus())) {
            throw new ConflictException("Sipariş faturalandırılabilir durumda değil "
                    + "(yalnızca Onaylandı, Kargoya Verildi veya Teslim Edildi durumundaki siparişler faturalandırılabilir)");
        }
        if (invoiceRepository.existsByOrder_Id(orderId)) {
            throw new ConflictException("Sipariş zaten faturalandırılmış");
        }
        if (cariAccountService.hasExistingDebtForReference(CariReferenceType.ORDER, orderId)) {
            throw new ConflictException("Bu sipariş için daha önce sipariş bazlı cari borç oluşturulmuş. "
                    + "Aynı borcun tekrar cari hesaba işlenmesi engellendi.");
        }

        User customer = order.getUser();
        validateCustomerBillingInfo(customer);
        validateSellerConfig();

        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setUser(customer);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(Instant.now());

        AppProperties.Company company = appProperties.getCompany();
        invoice.setSellerCompanyName(company.getName());
        invoice.setSellerTaxNumber(company.getTaxNumber());
        invoice.setSellerTaxOffice(company.getTaxOffice());
        invoice.setSellerAddress(company.getAddress());

        invoice.setCustomerCompanyName(billingName(customer));
        invoice.setCustomerTaxNumber(customer.getTaxNumber());
        invoice.setCustomerTaxOffice(customer.getTaxOffice());
        invoice.setCustomerAddress(customer.getBillingAddress());

        BigDecimal vatRate = appProperties.getInvoice().getDefaultVatRate();
        for (OrderItem oi : order.getItems()) {
            invoice.getItems().add(toInvoiceItem(invoice, oi, vatRate));
        }

        // invoice_number NOT NULL+UNIQUE olduğu için ilk INSERT'te henüz bilinmeyen gerçek numara
        // (id'ye bağlı) yerine geçici, garanti-benzersiz bir yer tutucu yazılır; id atandıktan sonra
        // asıl "FTR-{yıl}-{id}" numarası hesaplanıp ikinci bir save ile üzerine yazılır.
        invoice.setInvoiceNumber("TMP-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 20));

        Invoice saved;
        try {
            saved = invoiceRepository.save(invoice);
        } catch (DataIntegrityViolationException e) {
            // İki admin aynı siparişi eşzamanlı faturalandırmaya çalışırsa DB UNIQUE(order_id) son
            // güvenlik katmanı olarak devreye girer — servis katmanındaki existsByOrder_Id kontrolü
            // arada bir yarış koşuluyla atlanmış olsa bile mükerrer fatura burada engellenir.
            throw new ConflictException("Sipariş zaten faturalandırılmış");
        }

        saved.setInvoiceNumber(generateInvoiceNumber(saved));
        saved = invoiceRepository.save(saved);

        Map<Currency, BigDecimal> debtByCurrency = InvoiceCalculator.debtAmountsByCurrency(saved.getItems());
        cariAccountService.recordInvoiceDebt(customer.getId(), saved.getId(), debtByCurrency,
                "Fatura " + saved.getInvoiceNumber());

        return saved;
    }

    @Transactional
    public Invoice cancel(Integer invoiceId, Integer actingUserId) {
        Invoice invoice = getByIdAdmin(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new ConflictException("Bu fatura zaten iptal edilmiş");
        }

        // Ödeme kontrolü + ters cari hareket burada, tek transaction sınırı içinde yapılır — başarısız
        // olursa (ödeme varsa) hiçbir şey değişmeden ConflictException fırlar, Invoice.status güncellenmez.
        cariAccountService.reverseInvoiceDebt(invoice.getUser().getId(), invoice.getId(),
                "Fatura " + invoice.getInvoiceNumber() + " iptali", actingUserId);

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setCancelledAt(Instant.now());
        if (actingUserId != null) {
            invoice.setCancelledBy(userRepository.getReferenceById(actingUserId));
        }
        return invoiceRepository.save(invoice);
    }

    private InvoiceItem toInvoiceItem(Invoice invoice, OrderItem oi, BigDecimal vatRate) {
        InvoiceItem ii = new InvoiceItem();
        ii.setInvoice(invoice);
        ii.setProduct(oi.getProduct());
        ii.setProductCode(oi.getProductCode());
        ii.setProductName(oi.getProductName());
        ii.setQuantity(oi.getQty());
        ii.setUnitPrice(oi.getUnitPrice());
        ii.setLineTotal(oi.getTotalPrice());
        ii.setCurrency(oi.getCurrency());
        boolean includesVat = Boolean.TRUE.equals(oi.getPriceIncludesVat());
        ii.setPriceIncludesVat(includesVat);
        ii.setVatRate(vatRate);
        ii.setVatAmount(calculateVatAmount(oi.getTotalPrice(), vatRate, includesVat));
        return ii;
    }

    /** includesVat=true: lineTotal'in içinden gömülü KDV'yi ayrıştırır (bilgi amaçlı — lineTotal değişmez).
     * includesVat=false: lineTotal üzerine eklenecek KDV tutarını hesaplar. */
    private BigDecimal calculateVatAmount(BigDecimal lineTotal, BigDecimal vatRate, boolean includesVat) {
        BigDecimal rateFraction = vatRate.divide(HUNDRED, 6, RoundingMode.HALF_UP);
        if (includesVat) {
            BigDecimal net = lineTotal.divide(BigDecimal.ONE.add(rateFraction), 2, RoundingMode.HALF_UP);
            return lineTotal.subtract(net);
        }
        return lineTotal.multiply(rateFraction).setScale(2, RoundingMode.HALF_UP);
    }

    /** "FTR-{yıl}-{id, 4 haneye sıfırla doldurulmuş}" — Invoice.id'den türetilir, ayrı bir sayaçtan
     * DEĞİL (bkz. Invoice entity dokümantasyonu). Bu, resmi bir GİB/e-Fatura numarası değildir. */
    private String generateInvoiceNumber(Invoice invoice) {
        int year = invoice.getIssuedAt().atZone(ZoneId.systemDefault()).getYear();
        return String.format("FTR-%d-%04d", year, invoice.getId());
    }

    private String billingName(User user) {
        return isBlank(user.getCompanyName()) ? user.getDisplayName() : user.getCompanyName();
    }

    private void validateCustomerBillingInfo(User user) {
        List<String> missing = new ArrayList<>();
        if (isBlank(billingName(user))) missing.add("Firma/Ad Soyad");
        if (isBlank(user.getTaxNumber())) missing.add("Vergi No/TCKN");
        if (isBlank(user.getTaxOffice())) missing.add("Vergi Dairesi");
        if (isBlank(user.getBillingAddress())) missing.add("Fatura Adresi");
        if (!missing.isEmpty()) {
            throw new ConflictException("Müşterinin vergi bilgileri eksik, fatura oluşturulamıyor. Eksik alan(lar): "
                    + String.join(", ", missing) + ". Lütfen müşteri kaydını admin panelinden güncelleyin.");
        }
    }

    private void validateSellerConfig() {
        AppProperties.Company company = appProperties.getCompany();
        List<String> missing = new ArrayList<>();
        if (isBlank(company.getName())) missing.add("Şirket Adı");
        if (isBlank(company.getTaxNumber())) missing.add("Vergi No");
        if (isBlank(company.getTaxOffice())) missing.add("Vergi Dairesi");
        if (isBlank(company.getAddress())) missing.add("Adres");
        if (!missing.isEmpty()) {
            throw new ConflictException("Satıcı şirket bilgileri eksik yapılandırılmış (app.company.* — application.yml). "
                    + "Eksik alan(lar): " + String.join(", ", missing));
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
