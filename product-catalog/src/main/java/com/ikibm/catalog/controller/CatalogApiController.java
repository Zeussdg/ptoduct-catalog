package com.ikibm.catalog.controller;

import com.ikibm.catalog.dto.ProductLite;
import com.ikibm.catalog.repository.ProductRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Teklif sihirbazı combobox'ının kategoriye göre ürün çektiği hafif JSON uç noktası. */
@RestController
public class CatalogApiController {

    private final ProductRepository productRepository;

    public CatalogApiController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/api/catalog/products")
    public List<ProductLite> productsByCategory(@RequestParam String categorySlug) {
        return productRepository.findByCategory_SlugAndIsActiveTrueOrderByNameAsc(categorySlug)
                .stream().map(ProductLite::of).toList();
    }
}
