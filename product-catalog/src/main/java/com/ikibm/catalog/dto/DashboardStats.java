package com.ikibm.catalog.dto;

/** Admin dashboard KPI kartları için özet sayılar. */
public record DashboardStats(
        long totalQuotes,
        long pendingQuotes,
        long monthlyQuotes,
        long approvedQuotes,
        long rejectedQuotes,
        String totalAmountFormatted
) {
}
