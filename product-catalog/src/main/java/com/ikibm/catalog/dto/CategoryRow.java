package com.ikibm.catalog.dto;

/** Admin kategori tablosu/seçim listesi için düzleştirilmiş satır (girinti = depth). */
public record CategoryRow(Integer id, String name, String slug, Boolean isActive, int depth) {
}
