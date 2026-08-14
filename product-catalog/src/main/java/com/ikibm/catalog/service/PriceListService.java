package com.ikibm.catalog.service;

import com.ikibm.catalog.entity.PriceList;
import com.ikibm.catalog.repository.PriceListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PriceListService {

    private final PriceListRepository priceListRepository;

    public PriceListService(PriceListRepository priceListRepository) {
        this.priceListRepository = priceListRepository;
    }

    public List<PriceList> list() {
        return priceListRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public PriceList create(String name) {
        PriceList pl = new PriceList();
        pl.setName(name);
        pl.setIsActive(true);
        return priceListRepository.save(pl);
    }
}
