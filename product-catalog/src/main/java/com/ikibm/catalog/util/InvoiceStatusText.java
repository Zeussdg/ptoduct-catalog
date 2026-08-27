package com.ikibm.catalog.util;

import com.ikibm.catalog.entity.InvoiceStatus;
import org.springframework.stereotype.Component;

/** Fatura durumlarının Türkçe etiketleri — OrderStatusText ile aynı desen. Thymeleaf'te @invoiceStatus.label(...). */
@Component("invoiceStatus")
public class InvoiceStatusText {

    public String label(InvoiceStatus status) {
        if (status == null) return "";
        return switch (status) {
            case DRAFT -> "Taslak";
            case ISSUED -> "Kesildi";
            case CANCELLED -> "İptal Edildi";
        };
    }
}
