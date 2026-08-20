package com.ikibm.catalog.util;

import com.ikibm.catalog.entity.OrderStatus;
import org.springframework.stereotype.Component;

/** Sipariş durumlarının Türkçe etiketleri. Thymeleaf'te @orderStatus.label(...). */
@Component("orderStatus")
public class OrderStatusText {

    public String label(OrderStatus status) {
        if (status == null) return "";
        return switch (status) {
            case PENDING -> "Beklemede";
            case CONFIRMED -> "Onaylandı";
            case PREPARING -> "Hazırlanıyor";
            case SHIPPED -> "Kargoya Verildi";
            case DELIVERED -> "Teslim Edildi";
            case CANCELLED -> "İptal Edildi";
        };
    }

    public String label(String status) {
        try {
            return label(OrderStatus.valueOf(status));
        } catch (Exception e) {
            return status;
        }
    }
}
