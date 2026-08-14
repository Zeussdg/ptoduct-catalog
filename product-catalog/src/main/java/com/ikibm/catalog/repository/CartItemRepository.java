package com.ikibm.catalog.repository;

import com.ikibm.catalog.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {

    Optional<CartItem> findByCart_IdAndProduct_Id(Integer cartId, Integer productId);

    Optional<CartItem> findByIdAndCart_Id(Integer id, Integer cartId);

    boolean existsByProduct_Id(Integer productId);
}
