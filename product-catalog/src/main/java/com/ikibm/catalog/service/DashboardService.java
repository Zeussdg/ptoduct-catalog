package com.ikibm.catalog.service;

import com.ikibm.catalog.dto.DashboardStats;
import com.ikibm.catalog.dto.MonthlyQuoteStat;
import com.ikibm.catalog.dto.OrderDashboardStats;
import com.ikibm.catalog.dto.OrderStatusStat;
import com.ikibm.catalog.dto.QuoteStatusStat;
import com.ikibm.catalog.dto.TopCustomerStat;
import com.ikibm.catalog.dto.TopProductStat;
import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.entity.OrderStatus;
import com.ikibm.catalog.entity.QuoteStatus;
import com.ikibm.catalog.entity.User;
import com.ikibm.catalog.repository.OrderItemRepository;
import com.ikibm.catalog.repository.OrderRepository;
import com.ikibm.catalog.repository.QuoteItemRepository;
import com.ikibm.catalog.repository.QuoteRepository;
import com.ikibm.catalog.repository.UserRepository;
import com.ikibm.catalog.util.PriceFormatter;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Admin dashboard KPI/grafik verilerini aggregate repository sorgularıyla hesaplar. */
@Service
public class DashboardService {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter MONTH_LABEL =
            DateTimeFormatter.ofPattern("MMM", new Locale("tr", "TR"));
    private static final List<QuoteStatus> PENDING_LIKE = List.of(QuoteStatus.PENDING, QuoteStatus.REVIEWING);

    private final QuoteRepository quoteRepository;
    private final QuoteItemRepository quoteItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final PriceFormatter priceFormatter;

    public DashboardService(QuoteRepository quoteRepository, QuoteItemRepository quoteItemRepository,
                             OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                             UserRepository userRepository, PriceFormatter priceFormatter) {
        this.quoteRepository = quoteRepository;
        this.quoteItemRepository = quoteItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.priceFormatter = priceFormatter;
    }

    public DashboardStats stats() {
        Instant startOfMonth = LocalDate.now(ZONE).withDayOfMonth(1).atStartOfDay(ZONE).toInstant();
        long total = quoteRepository.count();
        long pending = quoteRepository.countByStatusIn(PENDING_LIKE);
        long monthly = quoteRepository.countByCreatedAtGreaterThanEqual(startOfMonth);
        long approved = quoteRepository.countByStatus(QuoteStatus.ACCEPTED);
        long rejected = quoteRepository.countByStatus(QuoteStatus.REJECTED);
        String totalAmount = formatCurrencySums(quoteItemRepository.sumAmountByCurrency(QuoteStatus.CANCELLED));
        return new DashboardStats(total, pending, monthly, approved, rejected, totalAmount);
    }

    public OrderDashboardStats orderStats() {
        Instant startOfMonth = LocalDate.now(ZONE).withDayOfMonth(1).atStartOfDay(ZONE).toInstant();
        long total = orderRepository.count();
        long pending = orderRepository.countByStatus(OrderStatus.PENDING);
        long monthly = orderRepository.countByCreatedAtGreaterThanEqual(startOfMonth);
        long delivered = orderRepository.countByStatus(OrderStatus.DELIVERED);
        long cancelled = orderRepository.countByStatus(OrderStatus.CANCELLED);
        String totalAmount = formatCurrencySums(orderItemRepository.sumAmountByCurrency(OrderStatus.CANCELLED));
        return new OrderDashboardStats(total, pending, monthly, delivered, cancelled, totalAmount);
    }

    /** Sistemde en az bir kaydı olan sipariş durumları, enum sırasında. */
    public List<OrderStatusStat> orderStatusDistribution() {
        Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        for (Object[] row : orderRepository.countGroupByStatus()) {
            counts.put((OrderStatus) row[0], (Long) row[1]);
        }
        return counts.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().ordinal()))
                .map(e -> new OrderStatusStat(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    /** Son 6 takvim ayı (bu ay dahil), veri olmayan aylar 0 ile doldurulur, kronolojik sırada. */
    public List<MonthlyQuoteStat> monthlyTrend() {
        YearMonth currentMonth = YearMonth.now(ZONE);
        YearMonth from = currentMonth.minusMonths(5);
        Instant fromInstant = from.atDay(1).atStartOfDay(ZONE).toInstant();

        Map<YearMonth, Long> counts = new LinkedHashMap<>();
        for (Object[] row : quoteRepository.countGroupByYearMonth(fromInstant)) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            long count = ((Number) row[2]).longValue();
            counts.put(YearMonth.of(year, month), count);
        }

        List<MonthlyQuoteStat> result = new ArrayList<>();
        for (int i = 0; i <= 5; i++) {
            YearMonth ym = from.plusMonths(i);
            String label = capitalize(MONTH_LABEL.format(ym.atDay(1)));
            result.add(new MonthlyQuoteStat(label, counts.getOrDefault(ym, 0L)));
        }
        return result;
    }

    /** Sistemde en az bir kaydı olan durumlar, enum sırasında. */
    public List<QuoteStatusStat> statusDistribution() {
        Map<QuoteStatus, Long> counts = new EnumMap<>(QuoteStatus.class);
        for (Object[] row : quoteRepository.countGroupByStatus()) {
            counts.put((QuoteStatus) row[0], (Long) row[1]);
        }
        return counts.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().ordinal()))
                .map(e -> new QuoteStatusStat(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public List<TopProductStat> topProducts(int limit) {
        List<TopProductStat> result = new ArrayList<>();
        for (Object[] row : quoteItemRepository.topProducts(PageRequest.of(0, limit))) {
            String name = (String) row[0];
            String code = (String) row[1];
            long offerCount = ((Number) row[2]).longValue();
            long totalQty = ((Number) row[3]).longValue();
            result.add(new TopProductStat(name, code, offerCount, totalQty));
        }
        return result;
    }

    public List<TopCustomerStat> topCustomers(int limit) {
        // Buffer alıp anonim satırla birleştikten sonra tekrar limit'e kesiyoruz.
        List<Object[]> countRows = quoteRepository.countGroupByUser(PageRequest.of(0, limit + 1));

        Map<Integer, Map<Currency, BigDecimal>> amountsByUser = new LinkedHashMap<>();
        for (Object[] row : quoteItemRepository.sumAmountByUserAndCurrency()) {
            Integer userId = (Integer) row[0];
            Currency currency = (Currency) row[1];
            BigDecimal amount = (BigDecimal) row[2];
            amountsByUser.computeIfAbsent(userId, k -> new EnumMap<>(Currency.class)).put(currency, amount);
        }

        List<Integer> userIds = countRows.stream().map(r -> (Integer) r[0]).collect(Collectors.toList());
        Map<Integer, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<TopCustomerStat> result = new ArrayList<>();
        for (Object[] row : countRows) {
            Integer userId = (Integer) row[0];
            long count = ((Number) row[1]).longValue();
            User user = usersById.get(userId);
            if (user == null) continue;
            String name = user.getCompanyName() != null ? user.getCompanyName() : user.getEmail();
            String amount = formatCurrencyMap(amountsByUser.getOrDefault(userId, Map.of()));
            result.add(new TopCustomerStat(name, count, amount));
        }

        long guestCount = quoteRepository.countByUserIsNull();
        if (guestCount > 0) {
            String guestAmount = formatCurrencySums(quoteItemRepository.sumAmountForGuestsByCurrency());
            result.add(new TopCustomerStat("Anonim (Misafir)", guestCount, guestAmount));
        }

        return result.stream()
                .sorted(Comparator.comparingLong(TopCustomerStat::offerCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private String formatCurrencySums(List<Object[]> rows) {
        Map<Currency, BigDecimal> map = new EnumMap<>(Currency.class);
        for (Object[] row : rows) {
            map.put((Currency) row[0], (BigDecimal) row[1]);
        }
        return formatCurrencyMap(map);
    }

    private String formatCurrencyMap(Map<Currency, BigDecimal> sums) {
        if (sums.isEmpty()) return "-";
        return sums.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().ordinal()))
                .map(e -> priceFormatter.format(e.getValue(), e.getKey()))
                .collect(Collectors.joining(" + "));
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
