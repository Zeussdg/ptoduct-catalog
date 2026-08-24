package com.ikibm.catalog.dto;

/** Bir ürün için vurgulanan fiyatın hangi kademeden geldiği (öncelik sırasıyla). */
public enum PriceSource {
    CUSTOMER, PRICE_LIST, SPECIAL, DEALER, LIST
}
