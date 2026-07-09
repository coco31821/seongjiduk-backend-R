package com.sungjiduk.backend.admin.dto.response;

public record AdminStatsOverviewResponse(
        long totalUsers,
        long todayVisitors,
        long tripPlanCount,
        String topContent,
        String topSpot
) {
}
