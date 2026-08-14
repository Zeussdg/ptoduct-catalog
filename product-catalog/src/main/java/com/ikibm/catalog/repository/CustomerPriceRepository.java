package com.ikibm.catalog.repository;

import com.ikibm.catalog.entity.CustomerPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerPriceRepository extends JpaRepository<CustomerPrice, Integer> {

    Optional<CustomerPrice> findByUser_IdAndProduct_Id(Integer userId, Integer productId);
}
