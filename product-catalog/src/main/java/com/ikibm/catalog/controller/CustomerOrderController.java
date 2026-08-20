package com.ikibm.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikibm.catalog.dto.QuotePdfRequest;
import com.ikibm.catalog.security.CatalogUserDetails;
import com.ikibm.catalog.service.OrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CustomerOrderController {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public CustomerOrderController(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/orders")
    public String create(@AuthenticationPrincipal CatalogUserDetails principal,
                         @RequestParam Integer quoteId) {
        var order = orderService.createFromQuote(principal.getId(), quoteId);
        return "redirect:/orders/" + order.getId();
    }

    @PostMapping("/orders/from-cart")
    public String createFromCart(@AuthenticationPrincipal CatalogUserDetails principal,
                                 @RequestParam("payload") String payload) throws Exception {
        QuotePdfRequest req = objectMapper.readValue(payload, QuotePdfRequest.class);
        var order = orderService.createFromCartItems(principal.getUser(), req.items());
        return "redirect:/orders/" + order.getId();
    }

    @GetMapping("/orders")
    public String list(@AuthenticationPrincipal CatalogUserDetails principal, Model model) {
        model.addAttribute("orders", orderService.listForUser(principal.getId()));
        return "public/orders";
    }

    @GetMapping("/orders/{id}")
    public String detail(@AuthenticationPrincipal CatalogUserDetails principal,
                         @PathVariable Integer id, Model model) {
        model.addAttribute("order", orderService.getForUser(principal.getId(), id));
        return "public/order-detail";
    }
}
