package com.sungjiduk.backend.admin.controller;

import com.sungjiduk.backend.admin.dto.response.AdminStatsOverviewResponse;
import com.sungjiduk.backend.admin.dto.response.StatsSeriesResponse;
import com.sungjiduk.backend.admin.service.AdminStatsService;
import com.sungjiduk.backend.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    public AdminStatsController(AdminStatsService adminStatsService) {
        this.adminStatsService = adminStatsService;
    }

    @GetMapping("/overview")
    public ApiResponse<AdminStatsOverviewResponse> overview() {
        return ApiResponse.ok(adminStatsService.overview());
    }

    @GetMapping("/visitors")
    public ApiResponse<StatsSeriesResponse> visitors() {
        return ApiResponse.ok(adminStatsService.visitors());
    }

    @GetMapping("/usage")
    public ApiResponse<StatsSeriesResponse> usage() {
        return ApiResponse.ok(adminStatsService.usage());
    }
}
