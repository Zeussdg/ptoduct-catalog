package com.ikibm.catalog.controller;

import com.ikibm.catalog.entity.Category;
import com.ikibm.catalog.entity.Product;
import com.ikibm.catalog.security.CatalogUserDetails;
import com.ikibm.catalog.service.ProductService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class ProductViewController {

    private final ProductService productService;

    public ProductViewController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/urun/{id}")
    public String detail(@PathVariable Integer id,
                         @AuthenticationPrincipal(errorOnInvalidType = false) CatalogUserDetails principal,
                         Model model) {
        Product product = productService.getById(id);
        Integer userId = principal != null ? principal.getId() : null;

        Category cat = product.getCategory();
        Category mainCat = null, subCat = null;
        if (cat != null) {
            if (cat.getParent() != null) { mainCat = cat.getParent(); subCat = cat; }
            else { mainCat = cat; }
        }

        List<Product> related = productService.related(product);

        model.addAttribute("product", product);
        model.addAttribute("mainCat", mainCat);
        model.addAttribute("subCat", subCat);
        model.addAttribute("related", related);
        model.addAttribute("price", productService.resolvePrice(product, userId));
        model.addAttribute("relatedPrices", productService.resolvePrices(related, userId));
        return "public/product-detail";
    }
}
