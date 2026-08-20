package com.ikibm.catalog.service;

import com.ikibm.catalog.dto.QuotePdfRequest;
import com.ikibm.catalog.entity.Order;
import com.ikibm.catalog.entity.OrderItem;
import com.ikibm.catalog.entity.OrderStatus;
import com.ikibm.catalog.entity.Quote;
import com.ikibm.catalog.entity.QuoteItem;
import com.ikibm.catalog.entity.User;
import com.ikibm.catalog.exception.ConflictException;
import com.ikibm.catalog.exception.NotFoundException;
import com.ikibm.catalog.repository.OrderRepository;
import com.ikibm.catalog.repository.QuoteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final QuoteRepository quoteRepository;
    private final QuoteService quoteService;

    public OrderService(OrderRepository orderRepository, QuoteRepository quoteRepository, QuoteService quoteService) {
        this.orderRepository = orderRepository;
        this.quoteRepository = quoteRepository;
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
                order.getItems().add(oi);
            }

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
