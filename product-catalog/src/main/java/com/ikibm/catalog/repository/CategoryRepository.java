package com.ikibm.catalog.repository;

import com.ikibm.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Category> findByParentIsNullOrderBySortOrderAsc();

    List<Category> findAllByOrderBySortOrderAsc();
}
