package com.ikibm.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/** Drawer'dan gelen teklif PDF isteği (payload JSON). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QuotePdfRequest(List<Item> items, Party seller, Party contact, double margin) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String name, String brand, String code, BigDecimal price, String currency, int qty) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Party(String firma, String yetkili, String telefon, String eposta) {}
}
