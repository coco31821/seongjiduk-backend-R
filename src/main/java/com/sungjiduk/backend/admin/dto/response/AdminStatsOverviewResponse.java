package com.sungjiduk.backend.admin.dto.response;

public record AdminStatsOverviewResponse(
        long totalUsers,
        long todayVisitors,
        long tripPlanCount,
        long aiRequestCount,
        String topContent,
        String topSpot
) {
}
