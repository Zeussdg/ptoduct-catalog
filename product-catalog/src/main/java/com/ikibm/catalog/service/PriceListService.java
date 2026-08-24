package com.ikibm.catalog.service;

import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.entity.PriceList;
import com.ikibm.catalog.entity.PriceListItem;
import com.ikibm.catalog.exception.NotFoundException;
import com.ikibm.catalog.repository.PriceListItemRepository;
import com.ikibm.catalog.repository.PriceListRepository;
import com.ikibm.catalog.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PriceListService {

    private final PriceListRepository priceListRepository;
    private final PriceListItemRepository priceListItemRepository;
    private final ProductRepository productRepository;

    public PriceListService(PriceListRepository priceListRepository, PriceListItemRepository priceListItemRepository,
                            ProductRepository productRepository) {
        this.priceListRepository = priceListRepository;
        this.priceListItemRepository = priceListItemRepository;
        this.productRepository = productRepository;
    }

    public List<PriceList> list() {
        return priceListRepository.findAllByOrderByCreatedAtDesc();
    }

    public PriceList get(Integer id) {
        return priceListRepository.findById(id).orElseThrow(() -> new NotFoundException("Fiyat listesi bulunamadı"));
    }

    @Transactional
    public PriceList create(String name) {
        PriceList pl = new PriceList();
        pl.setName(name);
        pl.setIsActive(true);
        return priceListRepository.save(pl);
    }

    public List<PriceListItem> items(Integer priceListId) {
        return priceListItemRepository.findByPriceList_IdOrderByCreatedAtDesc(priceListId);
    }

    /** Aynı (liste, ürün) için kayıt varsa fiyatını günceller, yoksa yeni ekler. */
    @Transactional
    public PriceListItem addItem(Integer priceListId, Integer productId, BigDecimal price, Currency currency) {
        PriceListItem item = priceListItemRepository.findByPriceList_IdAndProduct_Id(priceListId, productId).orElse(null);
        if (item == null) {
            item = new PriceListItem();
            item.setPriceList(priceListRepository.getReferenceById(priceListId));
            item.setProduct(productRepository.getReferenceById(productId));
        }
        item.setPrice(price);
        item.setCurrency(currency);
        return priceListItemRepository.save(item);
    }

    @Transactional
    public void removeItem(Integer itemId) {
        if (!priceListItemRepository.existsById(itemId)) {
            throw new NotFoundException("Fiyat listesi kalemi bulunamadı");
        }
        priceListItemRepository.deleteById(itemId);
    }

    /** Bir ürünün aktif fiyat listelerindeki tüm eşleşmeleri — sipariş düzenlemedeki fiyat seçimi için. */
    public List<PriceListItem> matchesForProduct(Integer productId) {
        return priceListItemRepository.findActiveByProduct_Id(productId);
    }
}
