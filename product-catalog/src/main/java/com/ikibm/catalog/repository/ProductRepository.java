package com.ikibm.catalog.repository;

import com.ikibm.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByStockCode(String stockCode);

    boolean existsByStockCode(String stockCode);

    List<Product> findTop4ByCategory_SlugAndIsActiveTrueAndIdNot(String slug, Integer id);

    List<Product> findByCategory_SlugAndIsActiveTrueOrderByNameAsc(String slug);

    @Query("select distinct p.brand from Product p where p.brand is not null and p.isActive = true order by p.brand")
    List<String> findDistinctBrands();

    long countByIsActiveTrue();

    @Query("select c.slug, count(p) from Product p join p.category c where p.isActive = true group by c.slug")
    List<Object[]> countActiveByCategorySlug();
}
