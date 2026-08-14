package com.ikibm.catalog.repository;

import com.ikibm.catalog.entity.PriceList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceListRepository extends JpaRepository<PriceList, Integer> {

    List<PriceList> findAllByOrderByCreatedAtDesc();
}
