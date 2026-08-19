package com.ikibm.catalog.dto;

import com.ikibm.catalog.entity.QuoteStatus;

/** Duruma göre teklif dağılımı grafiği için tek bir durum satırı. */
public record QuoteStatusStat(QuoteStatus status, long count) {
}
