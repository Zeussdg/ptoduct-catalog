package com.ikibm.catalog.controller;

import com.ikibm.catalog.entity.Category;
import com.ikibm.catalog.entity.Product;
import com.ikibm.catalog.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ProductViewController {

    private final ProductService productService;

    public ProductViewController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/urun/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        Product product = productService.getById(id);

        Category cat = product.getCategory();
        Category mainCat = null, subCat = null;
        if (cat != null) {
            if (cat.getParent() != null) { mainCat = cat.getParent(); subCat = cat; }
            else { mainCat = cat; }
        }

        model.addAttribute("product", product);
        model.addAttribute("mainCat", mainCat);
        model.addAttribute("subCat", subCat);
        model.addAttribute("related", productService.related(product));
        return "public/product-detail";
    }
}
