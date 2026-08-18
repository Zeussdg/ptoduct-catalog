package com.ikibm.catalog.service;

import com.ikibm.catalog.entity.Currency;
import com.ikibm.catalog.entity.CustomerPrice;
import com.ikibm.catalog.exception.NotFoundException;
import com.ikibm.catalog.repository.CustomerPriceRepository;
import com.ikibm.catalog.repository.ProductRepository;
import com.ikibm.catalog.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class CustomerPriceService {

    private final CustomerPriceRepository customerPriceRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CustomerPriceService(CustomerPriceRepository customerPriceRepository, UserRepository userRepository,
                                ProductRepository productRepository) {
        this.customerPriceRepository = customerPriceRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    public List<CustomerPrice> listForCustomer(Integer userId) {
        return customerPriceRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    /** Aynı (müşteri, ürün) için kayıt varsa günceller, yoksa yeni oluşturur — unique constraint'e çarpmadan tekrar tekrar çağrılabilir. */
    @Transactional
    public CustomerPrice create(Integer userId, Integer productId, BigDecimal price, Currency currency,
                                Instant validFrom, Instant validTo) {
        CustomerPrice cp = customerPriceRepository.findByUser_IdAndProduct_Id(userId, productId).orElse(null);
        if (cp == null) {
            cp = new CustomerPrice();
            cp.setUser(userRepository.getReferenceById(userId));
            cp.setProduct(productRepository.getReferenceById(productId));
        }
        cp.setPrice(price);
        cp.setCurrency(currency);
        cp.setValidFrom(validFrom);
        cp.setValidTo(validTo);
        return customerPriceRepository.save(cp);
    }

    public boolean exists(Integer userId, Integer productId) {
        return customerPriceRepository.findByUser_IdAndProduct_Id(userId, productId).isPresent();
    }

    @Transactional
    public void delete(Integer id) {
        if (!customerPriceRepository.existsById(id)) {
            throw new NotFoundException("Özel fiyat bulunamadı");
        }
        customerPriceRepository.deleteById(id);
    }
}
