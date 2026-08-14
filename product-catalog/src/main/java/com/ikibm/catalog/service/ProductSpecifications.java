package com.ikibm.catalog.service;

import com.ikibm.catalog.entity.Category;
import com.ikibm.catalog.entity.Product;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ProductSpecifications {

    private ProductSpecifications() {}

    public static Specification<Product> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    public static Specification<Product> search(String q) {
        if (q == null || q.isBlank()) return null;
        String like = "%" + q.toLowerCase() + "%";
        return (root, query, cb) -> {
            Join<Product, Category> cat = root.join("category", JoinType.LEFT);
            Join<Category, Category> parent = cat.join("parent", JoinType.LEFT);
            List<Predicate> ors = new ArrayList<>();
            ors.add(cb.like(cb.lower(root.get("name")), like));
            ors.add(cb.like(cb.lower(root.get("brand")), like));
            ors.add(cb.like(cb.lower(root.get("stockCode")), like));
            ors.add(cb.like(cb.lower(cat.get("name")), like));
            ors.add(cb.like(cb.lower(parent.get("name")), like));
            return cb.or(ors.toArray(new Predicate[0]));
        };
    }

    public static Specification<Product> brand(String brand) {
        if (brand == null || brand.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("brand"), brand);
    }

    /** altkategori: doğrudan alt kategori slug eşleşmesi. */
    public static Specification<Product> subCategory(String slug) {
        if (slug == null || slug.isBlank()) return null;
        return (root, query, cb) -> {
            Join<Product, Category> cat = root.join("category", JoinType.LEFT);
            return cb.equal(cat.get("slug"), slug);
        };
    }

    /** kategori: ana kategori slug (ürünün kategorisi ya da onun ebeveyni eşleşir). */
    public static Specification<Product> mainCategory(String slug) {
        if (slug == null || slug.isBlank()) return null;
        return (root, query, cb) -> {
            Join<Product, Category> cat = root.join("category", JoinType.LEFT);
            Join<Category, Category> parent = cat.join("parent", JoinType.LEFT);
            return cb.or(cb.equal(cat.get("slug"), slug), cb.equal(parent.get("slug"), slug));
        };
    }
}
