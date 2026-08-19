package com.ikibm.catalog.dto;

/** En aktif müşteriler listesi için tek bir müşteri satırı (anonim teklifler "Anonim (Misafir)" olarak temsil edilir). */
public record TopCustomerStat(String displayName, long offerCount, String totalAmountFormatted) {
}
