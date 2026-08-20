package com.ikibm.catalog.service;

import com.ikibm.catalog.dto.QuotePdfRequest;
import com.ikibm.catalog.entity.*;
import com.ikibm.catalog.exception.ConflictException;
import com.ikibm.catalog.exception.NotFoundException;
import com.ikibm.catalog.repository.CartItemRepository;
import com.ikibm.catalog.repository.QuoteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final CartService cartService;
    private final CartItemRepository cartItemRepository;

    public QuoteService(QuoteRepository quoteRepository, CartService cartService,
                        CartItemRepository cartItemRepository) {
        this.quoteRepository = quoteRepository;
        this.cartService = cartService;
        this.cartItemRepository = cartItemRepository;
    }

    /** Sepetten teklif oluştur (snapshot'lı) ve sepeti temizle — tek transaction. */
    @Transactional
    public Quote createFromCart(Integer userId) {
        Cart cart = cartService.getOrCreateCart(userId);
        if (cart.getItems().isEmpty()) {
            throw new ConflictException("Sepetiniz boş");
        }

        Quote quote = new Quote();
        quote.setUser(cart.getUser());
        quote.setStatus(QuoteStatus.PENDING);

        for (CartItem ci : cart.getItems()) {
            QuoteItem qi = new QuoteItem();
            qi.setQuote(quote);
            qi.setProduct(ci.getProduct());
            qi.setProductName(ci.getProduct().getName());
            qi.setProductCode(ci.getProduct().getStockCode());
            qi.setQty(ci.getQty());
            qi.setUnitPrice(ci.getUnitPrice());
            qi.setTotalPrice(ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQty())));
            qi.setCurrency(ci.getCurrency());
            quote.getItems().add(qi);
        }

        Quote saved = quoteRepository.save(quote);
        cartItemRepository.deleteAll(cart.getItems());
        return saved;
    }

    /** Sepet drawer'ının anonim "Teklifi PDF'e geçir" gönderiminden teklif kaydı oluşturur (snapshot'lı). */
    @Transactional
    public Quote createFromPdfRequest(QuotePdfRequest req, User authenticatedUserOrNull) {
        if (req.items() == null || req.items().isEmpty()) return null;

        Quote quote = new Quote();
        quote.setUser(authenticatedUserOrNull);
        quote.setStatus(QuoteStatus.PENDING);
        quote.setMarginPct(BigDecimal.valueOf(req.margin()));
        if (req.contact() != null) {
            quote.setRecipientCompany(req.contact().firma());
            quote.setRecipientContact(req.contact().yetkili());
            quote.setRecipientPhone(req.contact().telefon());
        }

        for (QuotePdfRequest.Item it : req.items()) {
            QuoteItem qi = new QuoteItem();
            qi.setQuote(quote);
            qi.setProductName(it.name());
            qi.setProductCode(it.code());
            qi.setQty(it.qty());
            BigDecimal unit = it.price() != null ? it.price() : BigDecimal.ZERO;
            qi.setUnitPrice(unit);
            qi.setTotalPrice(unit.multiply(BigDecimal.valueOf(it.qty())));
            qi.setCurrency(Currency.valueOf(it.currency()));
            quote.getItems().add(qi);
        }

        return quoteRepository.save(quote);
    }

    public List<Quote> listForUser(Integer userId) {
        return quoteRepository.findByUserIdWithoutOrder(userId);
    }

    public Quote getForUser(Integer userId, Integer quoteId) {
        return quoteRepository.findByIdAndUser_Id(quoteId, userId)
                .orElseThrow(() -> new NotFoundException("Teklif bulunamadı"));
    }

    // ---- admin ----

    public Page<Quote> listAll(String status, int page) {
        PageRequest pr = PageRequest.of(Math.max(0, page - 1), 50);
        if (status == null || status.isBlank()) {
            return quoteRepository.findAllByOrderByCreatedAtDesc(pr);
        }
        return quoteRepository.findByStatusOrderByCreatedAtDesc(QuoteStatus.valueOf(status), pr);
    }

    public Quote getByIdAdmin(Integer id) {
        return quoteRepository.findById(id).orElseThrow(() -> new NotFoundException("Teklif bulunamadı"));
    }

    @Transactional
    public Quote updateStatus(Integer id, String status, String adminNote) {
        Quote q = getByIdAdmin(id);
        q.setStatus(QuoteStatus.valueOf(status));
        q.setAdminNote(adminNote);
        return quoteRepository.save(q);
    }

    // ---- müşteri (bayi) ----

    /** Teklif, toptancıya değil bayinin kendi (3. taraf) müşterisine yapıldığından
     * durumu asıl bayi yönetir; sahiplik kontrolü getForUser üzerinden sağlanır. */
    @Transactional
    public Quote updateStatusForUser(Integer userId, Integer id, String status, String note) {
        Quote q = getForUser(userId, id);
        q.setStatus(QuoteStatus.valueOf(status));
        q.setNote(note);
        return quoteRepository.save(q);
    }

    public long countPending() { return quoteRepository.countByStatus(QuoteStatus.PENDING); }

    public List<Quote> recent5() { return quoteRepository.findTop5ByOrderByCreatedAtDesc(); }
}
