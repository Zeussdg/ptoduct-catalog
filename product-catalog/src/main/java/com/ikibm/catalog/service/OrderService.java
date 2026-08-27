package com.ikibm.catalog.service;

import com.ikibm.catalog.dto.OrderItemsUpdateRequest;
import com.ikibm.catalog.dto.QuotePdfRequest;
import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.entity.Order;
import com.ikibm.catalog.entity.OrderItem;
import com.ikibm.catalog.entity.OrderStatus;
import com.ikibm.catalog.entity.Product;
import com.ikibm.catalog.entity.Quote;
import com.ikibm.catalog.entity.QuoteItem;
import com.ikibm.catalog.entity.User;
import com.ikibm.catalog.exception.ConflictException;
import com.ikibm.catalog.exception.NotFoundException;
import com.ikibm.catalog.repository.OrderRepository;
import com.ikibm.catalog.repository.ProductRepository;
import com.ikibm.catalog.repository.QuoteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final QuoteRepository quoteRepository;
    private final ProductRepository productRepository;
    private final QuoteService quoteService;

    public OrderService(OrderRepository orderRepository, QuoteRepository quoteRepository,
                        ProductRepository productRepository, QuoteService quoteService) {
        this.orderRepository = orderRepository;
        this.quoteRepository = quoteRepository;
        this.productRepository = productRepository;
        this.quoteService = quoteService;
    }

    /** Sepetten (istemci/localStorage sepeti — sunucu Cart entity'si değil) tek adımda
     * teklif + sipariş oluşturur (drawer'daki "Sipariş Oluştur" kısayolu). Satıcı/alıcı/marj
     * formu olmadığı için QuotePdfRequest'i seller/contact=null, margin=0 ile kurup
     * QuoteService.createFromPdfRequest'i (aynı /quote/pdf akışının kullandığı metot) yeniden kullanır. */
    @Transactional
    public Order createFromCartItems(User user, List<QuotePdfRequest.Item> items) {
        if (items == null || items.isEmpty()) {
            throw new ConflictException("Sepetiniz boş");
        }
        Quote quote = quoteService.createFromPdfRequest(new QuotePdfRequest(items, null, null, 0.0), user);
        return createFromQuote(user.getId(), quote.getId());
    }

    /** Bir tekliften sipariş oluşturur (snapshot'lı, marj eklenmez — bayi bize ham fiyat + KDV öder).
     * Teklife zaten bağlı bir sipariş varsa (uq_orders_quote), onu döner — mükerrer kayıt oluşmaz. */
    @Transactional
    public Order createFromQuote(Integer userId, Integer quoteId) {
        return orderRepository.findByQuote_Id(quoteId).orElseGet(() -> {
            Quote quote = quoteRepository.findByIdAndUser_Id(quoteId, userId)
                    .orElseThrow(() -> new NotFoundException("Teklif bulunamadı"));

            Order order = new Order();
            order.setQuote(quote);
            order.setUser(quote.getUser());
            order.setStatus(OrderStatus.PENDING);

            for (QuoteItem qi : quote.getItems()) {
                OrderItem oi = new OrderItem();
                oi.setOrder(order);
                oi.setProduct(qi.getProduct());
                oi.setProductName(qi.getProductName());
                oi.setProductCode(qi.getProductCode());
                oi.setQty(qi.getQty());
                oi.setUnitPrice(qi.getUnitPrice());
                oi.setTotalPrice(qi.getTotalPrice());
                oi.setCurrency(qi.getCurrency());
                oi.setPriceIncludesVat(Boolean.TRUE.equals(qi.getPriceIncludesVat()));
                oi.setPriceListName(qi.getPriceListName());
                order.getItems().add(oi);
            }

            // NOT: Sipariş oluşturulduğunda cari hesaba HİÇBİR ŞEY yazılmaz — "Veresiye" fiyat listesinden
            // gelen kalemler dahil. Cari borç artık SADECE admin "Faturalandır" dediğinde, InvoiceService
            // üzerinden oluşturulur (bkz. InvoiceService.createFromOrder → CariAccountService.recordInvoiceDebt).
            // Eskiden burada sipariş oluşur oluşmaz otomatik borç yazan bir mekanizma vardı; onaylanmadan/
            // faturalandırılmadan cariye yansımaması gerektiği için kaldırıldı.
            return orderRepository.save(order);
        });
    }

    public List<Order> listForUser(Integer userId) {
        return orderRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    public Order getForUser(Integer userId, Integer id) {
        return orderRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new NotFoundException("Sipariş bulunamadı"));
    }

    /** Bir teklife zaten bağlı sipariş var mı — teklif detay sayfasının "Sipariş Oluştur" /
     * "Siparişi Görüntüle" kararını verebilmesi için. */
    public Optional<Order> findByQuoteId(Integer quoteId) {
        return orderRepository.findByQuote_Id(quoteId);
    }

    // ---- admin ----

    /** Admin sipariş detayındaki kalem düzenleme formunu uygular: silinenler orphanRemoval ile
     * kaldırılır, kalanlar aynı managed instance üzerinde güncellenir, yeni satırlar eklenir.
     * Yeni satırlarda ürün adı/kodu istemciden değil, doğrulanmış Product kaydından alınır. */
    @Transactional
    public Order updateItems(Integer orderId, List<OrderItemsUpdateRequest.Line> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new ConflictException("Siparişte en az bir ürün kalemi bulunmalıdır");
        }
        Order order = getByIdAdmin(orderId);

        Map<Integer, OrderItem> existingById = order.getItems().stream()
                .collect(Collectors.toMap(OrderItem::getId, Function.identity()));

        Set<Integer> keptIds = lines.stream()
                .map(OrderItemsUpdateRequest.Line::itemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        order.getItems().removeIf(oi -> !keptIds.contains(oi.getId()));

        List<Integer> newProductIds = lines.stream()
                .filter(l -> l.itemId() == null)
                .map(OrderItemsUpdateRequest.Line::productId)
                .distinct().toList();
        Map<Integer, Product> newProducts = productRepository.findAllById(newProductIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        for (OrderItemsUpdateRequest.Line line : lines) {
            if (line.qty() == null || line.qty() < 1) {
                throw new ConflictException("Adet en az 1 olmalıdır");
            }
            if (line.unitPrice() == null || line.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new ConflictException("Birim fiyat negatif olamaz");
            }
            Currency currency;
            try {
                currency = Currency.valueOf(line.currency());
            } catch (Exception e) {
                throw new ConflictException("Geçersiz para birimi");
            }

            OrderItem oi;
            if (line.itemId() != null) {
                oi = existingById.get(line.itemId());
                if (oi == null) throw new NotFoundException("Sipariş kalemi bulunamadı");
            } else {
                Product p = newProducts.get(line.productId());
                if (p == null) throw new NotFoundException("Ürün bulunamadı");
                oi = new OrderItem();
                oi.setOrder(order);
                oi.setProduct(p);
                oi.setProductName(p.getName());
                oi.setProductCode(p.getStockCode());
                order.getItems().add(oi);
            }
            oi.setQty(line.qty());
            oi.setUnitPrice(line.unitPrice());
            oi.setCurrency(currency);
            oi.setTotalPrice(line.unitPrice().multiply(BigDecimal.valueOf(line.qty())));
            oi.setPriceIncludesVat(Boolean.TRUE.equals(line.priceIncludesVat()));
        }

        return orderRepository.save(order);
    }

    public List<Order> recent5() {
        return orderRepository.findTop5ByOrderByCreatedAtDesc();
    }

    public Page<Order> listAll(String status, int page) {
        PageRequest pr = PageRequest.of(Math.max(0, page - 1), 50);
        if (status == null || status.isBlank()) {
            return orderRepository.findAllByOrderByCreatedAtDesc(pr);
        }
        return orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.valueOf(status), pr);
    }

    public Order getByIdAdmin(Integer id) {
        return orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Sipariş bulunamadı"));
    }

    @Transactional
    public Order updateStatus(Integer id, String status, String adminNote, String carrierName, String trackingNumber) {
        Order o = getByIdAdmin(id);
        o.setStatus(OrderStatus.valueOf(status));
        o.setAdminNote(adminNote);
        o.setCarrierName(carrierName);
        o.setTrackingNumber(trackingNumber);
        return orderRepository.save(o);
    }
}
