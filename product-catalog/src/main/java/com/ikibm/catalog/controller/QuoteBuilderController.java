package com.ikibm.catalog.controller;

import com.ikibm.catalog.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class QuoteBuilderController {

    private final CategoryService categoryService;

    public QuoteBuilderController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/teklif-sihirbazi")
    public String quoteBuilder(Model model) {
        model.addAttribute("categoryOptions", categoryService.categoryOptions());
        return "public/quote-builder";
    }
}
