package com.ikibm.catalog.controller;

import com.ikibm.catalog.security.CatalogUserDetails;
import com.ikibm.catalog.service.CartService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    public String cart(@AuthenticationPrincipal CatalogUserDetails principal, Model model) {
        model.addAttribute("cart", cartService.getOrCreateCart(principal.getId()));
        return "public/cart";
    }

    @PostMapping("/cart/items")
    public String addItem(@AuthenticationPrincipal CatalogUserDetails principal,
                          @RequestParam Integer productId,
                          @RequestParam(defaultValue = "1") int qty) {
        cartService.addItem(principal.getId(), productId, qty);
        return "redirect:/cart";
    }

    @PostMapping("/cart/items/{id}")
    public String updateItem(@AuthenticationPrincipal CatalogUserDetails principal,
                             @PathVariable Integer id, @RequestParam int qty) {
        cartService.updateItemQty(principal.getId(), id, qty);
        return "redirect:/cart";
    }

    @PostMapping("/cart/items/{id}/delete")
    public String removeItem(@AuthenticationPrincipal CatalogUserDetails principal, @PathVariable Integer id) {
        cartService.removeItem(principal.getId(), id);
        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String clear(@AuthenticationPrincipal CatalogUserDetails principal) {
        cartService.clearCart(principal.getId());
        return "redirect:/cart";
    }
}
