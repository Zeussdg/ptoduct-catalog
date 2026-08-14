package com.ikibm.catalog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.ikibm.catalog.dto.SeedCategory;
import com.ikibm.catalog.dto.SeedProduct;
import com.ikibm.catalog.entity.*;
import com.ikibm.catalog.repository.CategoryRepository;
import com.ikibm.catalog.repository.ProductRepository;
import com.ikibm.catalog.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * İlk açılışta (tablolar boşken) kategori + ürün verisini seed/*.json'dan yükler
 * ve SUPER_ADMIN yoksa bootstrap eder. server/prisma/seed.js'i yansıtır.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;
    private final ObjectMapper mapper;

    public DataSeeder(CategoryRepository categoryRepository, ProductRepository productRepository,
                      UserRepository userRepository, PasswordEncoder passwordEncoder,
                      AppProperties appProperties, ObjectMapper mapper) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        final Map<String, Category> slugToCategory = new HashMap<>();
        if (categoryRepository.count() == 0) {
            slugToCategory.putAll(seedCategories());
            log.info("Seed: {} kategori yüklendi", slugToCategory.size());
        }
        if (productRepository.count() == 0) {
            if (slugToCategory.isEmpty()) {
                categoryRepository.findAll().forEach(c -> slugToCategory.put(c.getSlug(), c));
            }
            int n = seedProducts(slugToCategory);
            log.info("Seed: {} ürün yüklendi", n);
        }
        seedBootstrapAdmin();
    }

    private Map<String, Category> seedCategories() throws Exception {
        List<SeedCategory> mains = readList("seed/categories.json", SeedCategory.class);
        Map<String, Category> map = new HashMap<>();
        int i = 0;
        for (SeedCategory main : mains) {
            Category mc = new Category();
            mc.setName(main.name());
            mc.setSlug(main.slug());
            mc.setSortOrder(i++);
            mc.setIsActive(true);
            mc = categoryRepository.save(mc);
            map.put(mc.getSlug(), mc);

            int j = 0;
            if (main.subCategories() != null) {
                for (SeedCategory sub : main.subCategories()) {
                    Category sc = new Category();
                    sc.setName(sub.name());
                    sc.setSlug(sub.slug());
                    sc.setSortOrder(j++);
                    sc.setIsActive(true);
                    sc.setParent(mc);
                    sc = categoryRepository.save(sc);
                    map.put(sc.getSlug(), sc);
                }
            }
        }
        return map;
    }

    private int seedProducts(Map<String, Category> slugToCategory) throws Exception {
        List<SeedProduct> items = readList("seed/products.json", SeedProduct.class);
        List<Product> batch = new ArrayList<>();
        for (SeedProduct sp : items) {
            String stockCode = (sp.stockCode() != null && !sp.stockCode().isBlank())
                    ? sp.stockCode() : "LEGACY-" + sp.id();
            if (productRepository.existsByStockCode(stockCode)) continue;

            Category cat = slugToCategory.get(sp.categorySlug());
            if (cat == null) cat = slugToCategory.get(sp.mainCategorySlug());

            Product p = new Product();
            p.setBrand(sp.brand());
            p.setName(sp.name());
            p.setStockCode(stockCode);
            p.setDescription(sp.description());
            p.setPrice(sp.price() != null ? sp.price() : BigDecimal.ZERO);
            p.setCurrency(parseCurrency(sp.currency()));
            p.setCategory(cat);
            p.setIsActive(true);
            batch.add(p);
        }
        productRepository.saveAll(batch);
        return batch.size();
    }

    private Currency parseCurrency(String c) {
        if (c == null) return Currency.TRY;
        try {
            return Currency.valueOf(c.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Currency.TRY;
        }
    }

    private void seedBootstrapAdmin() {
        if (userRepository.existsByRole(Role.SUPER_ADMIN)) return;
        User admin = new User();
        admin.setEmail(appProperties.getSeed().getSuperAdminEmail());
        admin.setPasswordHash(passwordEncoder.encode(appProperties.getSeed().getSuperAdminPassword()));
        admin.setRole(Role.SUPER_ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setName("Süper");
        admin.setSurname("Admin");
        userRepository.save(admin);
        log.info("Seed: bootstrap SUPER_ADMIN oluşturuldu ({})", admin.getEmail());
    }

    private <T> List<T> readList(String path, Class<T> type) throws Exception {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            CollectionType ct = mapper.getTypeFactory().constructCollectionType(List.class, type);
            return mapper.readValue(in, ct);
        }
    }
}
