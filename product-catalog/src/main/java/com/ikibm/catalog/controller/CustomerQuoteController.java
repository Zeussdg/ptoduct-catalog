package com.ikibm.catalog.controller;

import com.ikibm.catalog.security.CatalogUserDetails;
import com.ikibm.catalog.service.QuoteService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CustomerQuoteController {

    private final QuoteService quoteService;

    public CustomerQuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
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
        return "public/quote-detail";
    }
}
