package com.ikibm.catalog.dto;

/** Admin dashboard "Siparişler" KPI kartları için özet sayılar. */
public record OrderDashboardStats(
        long totalOrders,
        long pendingOrders,
        long monthlyOrders,
        long deliveredOrders,
        long cancelledOrders,
        String totalAmountFormatted
) {
}
