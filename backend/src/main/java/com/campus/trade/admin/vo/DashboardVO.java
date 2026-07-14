package com.campus.trade.admin.vo;

public record DashboardVO(
        long totalUsers,
        long totalProducts,
        long totalOrders,
        long completedOrders,
        long todayNewUsers,
        long todayNewProducts,
        long todayNewOrders,
        long pendingReviewProducts
) {
}
