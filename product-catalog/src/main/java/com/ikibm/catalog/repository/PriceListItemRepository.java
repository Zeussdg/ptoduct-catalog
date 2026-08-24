package com.ikibm.catalog.repository;

import com.ikibm.catalog.entity.PriceListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PriceListItemRepository extends JpaRepository<PriceListItem, Integer> {

    List<PriceListItem> findByPriceList_IdOrderByCreatedAtDesc(Integer priceListId);

    Optional<PriceListItem> findByPriceList_IdAndProduct_Id(Integer priceListId, Integer productId);

    /** Bir ürünün aktif fiyat listelerindeki tüm eşleşmeleri — sipariş düzenleme ekranındaki
     * fiyat seçim listesi için (birden fazla liste aynı ürünü içerebilir). */
    @Query("select pli from PriceListItem pli join fetch pli.priceList " +
            "where pli.product.id = :productId and pli.priceList.isActive = true " +
            "order by pli.priceList.name asc")
    List<PriceListItem> findActiveByProduct_Id(@Param("productId") Integer productId);
}
