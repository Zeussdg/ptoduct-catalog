package com.ikibm.catalog.controller.admin;

import com.ikibm.catalog.service.QuoteService;
import com.ikibm.catalog.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/customers")
public class AdminCustomerController {

    private final UserService userService;
    private final QuoteService quoteService;

    public AdminCustomerController(UserService userService, QuoteService quoteService) {
        this.userService = userService;
        this.quoteService = quoteService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("customers", userService.listCustomers());
        return "admin/customers";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("customer", userService.getById(id));
        model.addAttribute("quotes", quoteService.listForUser(id));
        return "admin/customer-detail";
    }
}
