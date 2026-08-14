package com.ikibm.catalog.service;

import com.ikibm.catalog.dto.CategoryNode;
import com.ikibm.catalog.dto.CategoryOption;
import com.ikibm.catalog.dto.CategoryRow;
import com.ikibm.catalog.entity.Category;
import com.ikibm.catalog.exception.ConflictException;
import com.ikibm.catalog.exception.NotFoundException;
import com.ikibm.catalog.repository.CategoryRepository;
import com.ikibm.catalog.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public List<Category> allOrdered() {
        return categoryRepository.findAllByOrderBySortOrderAsc();
    }

    /** Ana kategoriler → alt kategoriler ağacı; her düğümde aktif ürün sayısı. */
    public List<CategoryNode> sidebar() {
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : productRepository.countActiveByCategorySlug()) {
            counts.put((String) row[0], (Long) row[1]);
        }

        List<Category> roots = categoryRepository.findByParentIsNullOrderBySortOrderAsc();
        return roots.stream().map(root -> {
            CategoryNode node = new CategoryNode(root.getName(), root.getSlug());
            long total = counts.getOrDefault(root.getSlug(), 0L);
            for (Category child : root.getChildren()) {
                long childCount = counts.getOrDefault(child.getSlug(), 0L);
                CategoryNode childNode = new CategoryNode(child.getName(), child.getSlug());
                childNode.setCount(childCount);
                node.addChild(childNode);
                total += childCount;
            }
            node.setCount(total);
            return node;
        }).toList();
    }

    public long totalCount() {
        return categoryRepository.count();
    }

    /** Teklif sihirbazı için "Ana › Alt" seçenekleri (value = alt kategori slug). */
    public List<CategoryOption> categoryOptions() {
        List<CategoryOption> opts = new ArrayList<>();
        for (Category main : categoryRepository.findByParentIsNullOrderBySortOrderAsc()) {
            for (Category sub : main.getChildren()) {
                opts.add(new CategoryOption(sub.getSlug(), main.getName() + " › " + sub.getName()));
            }
        }
        return opts;
    }

    /** Admin: ana→alt sırayla düzleştirilmiş kategori satırları (girinti = depth). */
    public List<CategoryRow> flatRows() {
        List<CategoryRow> rows = new ArrayList<>();
        for (Category main : categoryRepository.findByParentIsNullOrderBySortOrderAsc()) {
            rows.add(new CategoryRow(main.getId(), main.getName(), main.getSlug(), main.getIsActive(), 0));
            for (Category sub : main.getChildren()) {
                rows.add(new CategoryRow(sub.getId(), sub.getName(), sub.getSlug(), sub.getIsActive(), 1));
            }
        }
        return rows;
    }

    @Transactional
    public Category create(String name, String slug, Integer parentId) {
        if (categoryRepository.existsBySlug(slug)) {
            throw new ConflictException("Bu slug zaten kullanımda");
        }
        Category c = new Category();
        c.setName(name);
        c.setSlug(slug);
        c.setIsActive(true);
        c.setSortOrder(0);
        if (parentId != null) c.setParent(categoryRepository.getReferenceById(parentId));
        return categoryRepository.save(c);
    }

    @Transactional
    public void toggleActive(Integer id) {
        Category c = categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Kategori bulunamadı"));
        c.setIsActive(!Boolean.TRUE.equals(c.getIsActive()));
        categoryRepository.save(c);
    }

    @Transactional
    public void delete(Integer id) {
        Category c = categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Kategori bulunamadı"));
        try {
            categoryRepository.delete(c);
            categoryRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Bu kategori kullanımda, silinemedi");
        }
    }

    /** Toolbar başlığı: seçili alt/ana kategorinin adı, yoksa "Tüm Ürünler". */
    public String label(String kategori, String altkategori) {
        if (altkategori != null && !altkategori.isBlank()) {
            return categoryRepository.findBySlug(altkategori).map(Category::getName).orElse("Tüm Ürünler");
        }
        if (kategori != null && !kategori.isBlank()) {
            return categoryRepository.findBySlug(kategori).map(Category::getName).orElse("Tüm Ürünler");
        }
        return "Tüm Ürünler";
    }
}
