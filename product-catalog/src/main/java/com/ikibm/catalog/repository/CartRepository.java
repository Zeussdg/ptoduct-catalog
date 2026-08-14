package com.ikibm.catalog.repository;

import com.ikibm.catalog.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Integer> {

    Optional<Cart> findByUser_Id(Integer userId);
}
