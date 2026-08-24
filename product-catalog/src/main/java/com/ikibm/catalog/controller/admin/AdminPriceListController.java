package com.ikibm.catalog.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikibm.catalog.dto.PriceListItemsUpdateRequest;
import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.service.PriceListService;
import com.ikibm.catalog.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/price-lists")
public class AdminPriceListController {

    private final PriceListService priceListService;
    private final ProductService productService;
    private final ObjectMapper objectMapper;

    public AdminPriceListController(PriceListService priceListService, ProductService productService,
                                    ObjectMapper objectMapper) {
        this.priceListService = priceListService;
        this.productService = productService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("priceLists", priceListService.list());
        return "admin/price-lists";
    }

    @PostMapping
    public String create(@RequestParam String name) {
        priceListService.create(name);
        return "redirect:/admin/price-lists";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, @RequestParam(required = false) String q, Model model) {
        model.addAttribute("priceList", priceListService.get(id));
        model.addAttribute("items", priceListService.items(id));
        model.addAttribute("q", q);
        model.addAttribute("productResults", (q == null || q.isBlank()) ? List.of() : productService.adminList(q));
        return "admin/price-list-detail";
    }

    @PostMapping("/{id}/items")
    public String addItems(@PathVariable Integer id, @RequestParam("payload") String payload, RedirectAttributes ra) {
        try {
            PriceListItemsUpdateRequest req = objectMapper.readValue(payload, PriceListItemsUpdateRequest.class);
            if (req.items() != null) {
                for (PriceListItemsUpdateRequest.Item it : req.items()) {
                    priceListService.addItem(id, it.productId(), it.price(), Currency.valueOf(it.currency()));
                }
            }
            ra.addFlashAttribute("message", "Ürünler fiyat listesine eklendi");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Eklenemedi: geçersiz veri");
        }
        return "redirect:/admin/price-lists/" + id;
    }

    @PostMapping("/{id}/items/{itemId}/delete")
    public String deleteItem(@PathVariable Integer id, @PathVariable Integer itemId) {
        priceListService.removeItem(itemId);
        return "redirect:/admin/price-lists/" + id;
    }
}
