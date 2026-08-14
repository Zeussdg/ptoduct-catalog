package com.ikibm.catalog.util;

import com.ikibm.catalog.entity.QuoteStatus;
import org.springframework.stereotype.Component;

/** Teklif durumlarının Türkçe etiketleri. Thymeleaf'te @quoteStatus.label(...). */
@Component("quoteStatus")
public class QuoteStatusText {

    public String label(QuoteStatus status) {
        if (status == null) return "";
        return switch (status) {
            case PENDING -> "Beklemede";
            case REVIEWING -> "İnceleniyor";
            case OFFERED -> "Teklif Verildi";
            case ACCEPTED -> "Kabul Edildi";
            case REJECTED -> "Reddedildi";
            case CANCELLED -> "İptal Edildi";
        };
    }

    public String label(String status) {
        try {
            return label(QuoteStatus.valueOf(status));
        } catch (Exception e) {
            return status;
        }
    }
}
