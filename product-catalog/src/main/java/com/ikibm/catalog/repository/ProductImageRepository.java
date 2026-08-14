package com.ikibm.catalog.repository;

import com.ikibm.catalog.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {

    List<ProductImage> findByProduct_IdOrderBySortOrderAsc(Integer productId);

    Optional<ProductImage> findByIdAndProduct_Id(Integer id, Integer productId);

    @Query("select coalesce(max(i.sortOrder), -1) from ProductImage i where i.product.id = :productId")
    int maxSortOrder(@Param("productId") Integer productId);

    @Modifying
    @Query("update ProductImage i set i.isPrimary = false where i.product.id = :productId")
    void clearPrimary(@Param("productId") Integer productId);
}
