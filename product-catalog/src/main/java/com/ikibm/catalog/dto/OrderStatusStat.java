package com.ikibm.catalog.dto;

import com.ikibm.catalog.entity.OrderStatus;

/** Duruma göre sipariş dağılımı grafiği için tek bir durum satırı. */
public record OrderStatusStat(OrderStatus status, long count) {
}
