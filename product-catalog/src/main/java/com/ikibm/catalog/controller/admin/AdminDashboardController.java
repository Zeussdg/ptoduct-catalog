package com.ikibm.catalog.controller.admin;

import com.ikibm.catalog.dto.MonthlyQuoteStat;
import com.ikibm.catalog.dto.OrderStatusStat;
import com.ikibm.catalog.dto.QuoteStatusStat;
import com.ikibm.catalog.dto.TopCustomerStat;
import com.ikibm.catalog.dto.TopProductStat;
import com.ikibm.catalog.service.DashboardService;
import com.ikibm.catalog.service.OrderService;
import com.ikibm.catalog.service.ProductService;
import com.ikibm.catalog.service.QuoteService;
import com.ikibm.catalog.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Instant;
import java.util.List;
import java.util.function.ToLongFunction;

@Controller
public class AdminDashboardController {

    private static final int TOP_LIST_LIMIT = 10;

    private final ProductService productService;
    private final UserService userService;
    private final QuoteService quoteService;
    private final OrderService orderService;
    private final DashboardService dashboardService;

    public AdminDashboardController(ProductService productService, UserService userService,
                                     QuoteService quoteService, OrderService orderService,
                                     DashboardService dashboardService) {
        this.productService = productService;
        this.userService = userService;
        this.quoteService = quoteService;
        this.orderService = orderService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("totalProducts", productService.totalCount());
        model.addAttribute("totalCustomers", userService.countCustomers());
        model.addAttribute("pendingQuotes", quoteService.countPending());
        model.addAttribute("recentQuotes", quoteService.recent5());

        List<MonthlyQuoteStat> monthlyTrend = dashboardService.monthlyTrend();
        List<QuoteStatusStat> statusDistribution = dashboardService.statusDistribution();
        List<TopProductStat> topProducts = dashboardService.topProducts(TOP_LIST_LIMIT);
        List<TopCustomerStat> topCustomers = dashboardService.topCustomers(TOP_LIST_LIMIT);
        List<OrderStatusStat> orderStatusDistribution = dashboardService.orderStatusDistribution();

        model.addAttribute("stats", dashboardService.stats());
        model.addAttribute("monthlyTrend", monthlyTrend);
        model.addAttribute("statusDistribution", statusDistribution);
        model.addAttribute("topProducts", topProducts);
        model.addAttribute("topCustomers", topCustomers);
        model.addAttribute("orderStats", dashboardService.orderStats());
        model.addAttribute("orderStatusDistribution", orderStatusDistribution);
        model.addAttribute("recentOrders", orderService.recent5());
        // Bar chart genişlik/yükseklik yüzdeleri için: Thymeleaf'in #aggregates.max'i
        // ArrayList üzerinde SpEL method-resolution hatası verdiğinden, max'lar burada hesaplanır.
        model.addAttribute("maxMonthlyCount", maxOf(monthlyTrend, MonthlyQuoteStat::count));
        model.addAttribute("maxStatusCount", maxOf(statusDistribution, QuoteStatusStat::count));
        model.addAttribute("maxProductQty", maxOf(topProducts, TopProductStat::totalQty));
        model.addAttribute("maxCustomerCount", maxOf(topCustomers, TopCustomerStat::offerCount));
        model.addAttribute("maxOrderStatusCount", maxOf(orderStatusDistribution, OrderStatusStat::count));
        model.addAttribute("generatedAt", Instant.now());
        return "admin/dashboard";
    }

    private static <T> long maxOf(List<T> items, ToLongFunction<T> extractor) {
        return items.stream().mapToLong(extractor).max().orElse(0);
    }
}
