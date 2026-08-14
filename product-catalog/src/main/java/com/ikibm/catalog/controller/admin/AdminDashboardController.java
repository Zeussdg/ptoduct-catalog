package com.ikibm.catalog.controller.admin;

import com.ikibm.catalog.service.ProductService;
import com.ikibm.catalog.service.QuoteService;
import com.ikibm.catalog.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardController {

    private final ProductService productService;
    private final UserService userService;
    private final QuoteService quoteService;

    public AdminDashboardController(ProductService productService, UserService userService, QuoteService quoteService) {
        this.productService = productService;
        this.userService = userService;
        this.quoteService = quoteService;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("totalProducts", productService.totalCount());
        model.addAttribute("totalCustomers", userService.countCustomers());
        model.addAttribute("pendingQuotes", quoteService.countPending());
        model.addAttribute("recentQuotes", quoteService.recent5());
        return "admin/dashboard";
    }
}
