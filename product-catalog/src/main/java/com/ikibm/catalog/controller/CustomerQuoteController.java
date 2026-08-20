package com.ikibm.catalog.controller;

import com.ikibm.catalog.security.CatalogUserDetails;
import com.ikibm.catalog.service.AuditLogService;
import com.ikibm.catalog.service.OrderService;
import com.ikibm.catalog.service.QuoteService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CustomerQuoteController {

    private final QuoteService quoteService;
    private final AuditLogService auditLogService;
    private final OrderService orderService;

    public CustomerQuoteController(QuoteService quoteService, AuditLogService auditLogService, OrderService orderService) {
        this.quoteService = quoteService;
        this.auditLogService = auditLogService;
        this.orderService = orderService;
    }

    @PostMapping("/quotes")
    public String createFromCart(@AuthenticationPrincipal CatalogUserDetails principal) {
        var quote = quoteService.createFromCart(principal.getId());
        return "redirect:/quotes/" + quote.getId();
    }

    @GetMapping("/quotes")
    public String list(@AuthenticationPrincipal CatalogUserDetails principal, Model model) {
        model.addAttribute("quotes", quoteService.listForUser(principal.getId()));
        return "public/quotes";
    }

    @GetMapping("/quotes/{id}")
    public String detail(@AuthenticationPrincipal CatalogUserDetails principal,
                         @PathVariable Integer id, Model model) {
        model.addAttribute("quote", quoteService.getForUser(principal.getId(), id));
        model.addAttribute("order", orderService.findByQuoteId(id).orElse(null));
        return "public/quote-detail";
    }

    @PostMapping("/quotes/{id}/status")
    public String updateStatus(@AuthenticationPrincipal CatalogUserDetails principal,
                               @PathVariable Integer id, @RequestParam String status,
                               @RequestParam(required = false) String note) {
        quoteService.updateStatusForUser(principal.getId(), id, status, note);
        auditLogService.record(principal.getId(), "QUOTE_STATUS_CHANGED", "Quote", String.valueOf(id), null);
        return "redirect:/quotes/" + id;
    }
}
