package com.sungjiduk.backend.admin.service;

import com.sungjiduk.backend.admin.dto.response.AdminStatsOverviewResponse;
import com.sungjiduk.backend.admin.dto.response.StatsSeriesResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminStatsService {

    public AdminStatsOverviewResponse overview() {
        return new AdminStatsOverviewResponse(120, 34, 88, 103, "러브라이브! 뮤즈", "쇼헤이바시");
    }

    public StatsSeriesResponse visitors() {
        return new StatsSeriesResponse("visitors", List.of(
                new StatsSeriesResponse.Point("2026-07-01", 34),
                new StatsSeriesResponse.Point("2026-07-02", 41)
        ));
    }

    public StatsSeriesResponse usage() {
        return new StatsSeriesResponse("trip_plans", List.of(
                new StatsSeriesResponse.Point("trip_generate", 88),
                new StatsSeriesResponse.Point("ai_request", 103)
        ));
    }
}
