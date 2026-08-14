package com.ikibm.catalog.controller.admin;

import com.ikibm.catalog.service.PriceListService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/price-lists")
public class AdminPriceListController {

    private final PriceListService priceListService;

    public AdminPriceListController(PriceListService priceListService) {
        this.priceListService = priceListService;
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
}
