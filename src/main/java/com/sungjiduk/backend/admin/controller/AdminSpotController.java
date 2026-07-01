package com.sungjiduk.backend.admin.controller;

import com.sungjiduk.backend.admin.dto.request.AdminSpotUpsertRequest;
import com.sungjiduk.backend.admin.dto.request.SpotReportProcessRequest;
import com.sungjiduk.backend.admin.dto.response.AdminCommandResponse;
import com.sungjiduk.backend.admin.dto.response.SpotReportAdminResponse;
import com.sungjiduk.backend.admin.service.AdminSpotService;
import com.sungjiduk.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminSpotController {

    private final AdminSpotService adminSpotService;

    public AdminSpotController(AdminSpotService adminSpotService) {
        this.adminSpotService = adminSpotService;
    }

    @PostMapping("/spots")
    public ApiResponse<AdminCommandResponse> create(@Valid @RequestBody AdminSpotUpsertRequest request) {
        return ApiResponse.ok(adminSpotService.create(request));
    }

    @PatchMapping("/spots/{spotId}")
    public ApiResponse<AdminCommandResponse> update(@PathVariable Long spotId, @Valid @RequestBody AdminSpotUpsertRequest request) {
        return ApiResponse.ok(adminSpotService.update(spotId, request));
    }

    @DeleteMapping("/spots/{spotId}")
    public ApiResponse<AdminCommandResponse> delete(@PathVariable Long spotId) {
        return ApiResponse.ok(adminSpotService.delete(spotId));
    }

    @GetMapping("/spot-reports")
    public ApiResponse<List<SpotReportAdminResponse>> reports() {
        return ApiResponse.ok(adminSpotService.findReports());
    }

    @PatchMapping("/spot-reports/{reportId}")
    public ApiResponse<AdminCommandResponse> processReport(@PathVariable Long reportId, @Valid @RequestBody SpotReportProcessRequest request) {
        return ApiResponse.ok(adminSpotService.processReport(reportId, request));
    }
}
