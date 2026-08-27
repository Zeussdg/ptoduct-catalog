package com.ikibm.catalog.service;

import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.entity.Order;
import com.ikibm.catalog.entity.Quote;
import com.ikibm.catalog.entity.QuoteItem;
import com.ikibm.catalog.entity.User;
import com.ikibm.catalog.repository.OrderRepository;
import com.ikibm.catalog.repository.ProductRepository;
import com.ikibm.catalog.repository.QuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** Sipariş oluşturmanın temel davranışını doğrular. ÖNEMLİ: Sipariş oluşturulduğunda cari hesaba
 * artık HİÇBİR ŞEY yazılmıyor — "Veresiye" fiyat listesinden gelen kalemler dahil (bkz. OrderService
 * içindeki not). Cari borç sadece admin "Faturalandır" dediğinde InvoiceService üzerinden oluşur
 * (bkz. InvoiceServiceTest). OrderService artık CariAccountService'e hiç bağımlı değil. */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private QuoteRepository quoteRepository;
    @Mock private ProductRepository productRepository;
    @Mock private QuoteService quoteService;

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(orderRepository, quoteRepository, productRepository, quoteService);
        lenient().when(orderRepository.findByQuote_Id(anyInt())).thenReturn(Optional.empty());
        lenient().when(orderRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> {
            var order = inv.getArgument(0, Order.class);
            order.setId(99);
            return order;
        });
    }

    private QuoteItem item(String priceListName, BigDecimal total, Currency currency) {
        QuoteItem qi = new QuoteItem();
        qi.setProductName("Ürün");
        qi.setProductCode("KOD1");
        qi.setQty(1);
        qi.setUnitPrice(total);
        qi.setTotalPrice(total);
        qi.setCurrency(currency);
        qi.setPriceListName(priceListName);
        return qi;
    }

    @Test
    void createFromQuote_veresiyeItem_isCopiedToOrderAsPlainItem_noAutomaticDebt() {
        User user = new User();
        user.setId(4);
        Quote quote = new Quote();
        quote.setId(10);
        quote.setUser(user);
        quote.getItems().add(item("Veresiye", new BigDecimal("12500.00"), Currency.USD));
        when(quoteRepository.findByIdAndUser_Id(10, 4)).thenReturn(Optional.of(quote));

        Order order = service.createFromQuote(4, 10);

        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getPriceListName()).isEqualTo("Veresiye");
        assertThat(order.getItems().get(0).getTotalPrice()).isEqualByComparingTo("12500.00");
        // Not: CariAccountService bağımlılığı artık hiç yok — bu testin mock'lanacak bir cari
        // servisi olmaması bizzat "sipariş anında borç yazılmıyor" iddiasının kanıtıdır.
    }

    @Test
    void createFromQuote_guestOrder_noUser_stillCreatesOrder() {
        Quote quote = new Quote();
        quote.setId(13);
        quote.setUser(null);
        quote.getItems().add(item("Veresiye", new BigDecimal("100.00"), Currency.TRY));
        when(quoteRepository.findByIdAndUser_Id(13, null)).thenReturn(Optional.of(quote));

        Order order = service.createFromQuote(null, 13);

        assertThat(order.getId()).isEqualTo(99);
        assertThat(order.getUser()).isNull();
    }

    @Test
    void createFromQuote_alreadyExistingOrder_returnsExistingWithoutDuplicating() {
        var existing = new Order();
        existing.setId(77);
        when(orderRepository.findByQuote_Id(14)).thenReturn(Optional.of(existing));

        var result = service.createFromQuote(4, 14);

        assertThat(result.getId()).isEqualTo(77);
    }
}
