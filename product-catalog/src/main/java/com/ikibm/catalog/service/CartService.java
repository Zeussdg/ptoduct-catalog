package com.ikibm.catalog.service;

import com.ikibm.catalog.dto.ResolvedPrice;
import com.ikibm.catalog.entity.*;
import com.ikibm.catalog.exception.NotFoundException;
import com.ikibm.catalog.repository.CartItemRepository;
import com.ikibm.catalog.repository.CartRepository;
import com.ikibm.catalog.repository.ProductRepository;
import com.ikibm.catalog.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                       ProductRepository productRepository, UserRepository userRepository,
                       ProductService productService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.productService = productService;
    }

    @Transactional
    public Cart getOrCreateCart(Integer userId) {
        return cartRepository.findByUser_Id(userId).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(userRepository.getReferenceById(userId));
            return cartRepository.save(cart);
        });
    }

    @Transactional
    public Cart addItem(Integer userId, Integer productId, int qty) {
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(productId)
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .orElseThrow(() -> new NotFoundException("Ürün bulunamadı"));

        ResolvedPrice resolved = productService.resolvePrice(product, userId);
        CartItem item = cartItemRepository.findByCart_IdAndProduct_Id(cart.getId(), productId).orElse(null);
        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQty(Math.max(1, qty));
            item.setUnitPrice(resolved.price());
            item.setCurrency(resolved.currency());
        } else {
            item.setQty(item.getQty() + Math.max(1, qty));
        }
        cartItemRepository.save(item);
        return getOrCreateCart(userId);
    }

    @Transactional
    public Cart updateItemQty(Integer userId, Integer itemId, int qty) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByIdAndCart_Id(itemId, cart.getId())
                .orElseThrow(() -> new NotFoundException("Sepet kalemi bulunamadı"));
        item.setQty(Math.max(1, qty));
        cartItemRepository.save(item);
        return getOrCreateCart(userId);
    }

    @Transactional
    public Cart removeItem(Integer userId, Integer itemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByIdAndCart_Id(itemId, cart.getId())
                .orElseThrow(() -> new NotFoundException("Sepet kalemi bulunamadı"));
        cartItemRepository.delete(item);
        return getOrCreateCart(userId);
    }

    @Transactional
    public void clearCart(Integer userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteAll(cart.getItems());
    }
}
