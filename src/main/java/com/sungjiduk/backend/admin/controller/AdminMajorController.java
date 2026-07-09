package com.sungjiduk.backend.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sungjiduk.backend.admin.dto.request.AdminMajorContentSpotRequest;
import com.sungjiduk.backend.admin.dto.request.AdminMajorUserRateRequest;
import com.sungjiduk.backend.admin.dto.response.AdminMajorContentSpotResponse;
import com.sungjiduk.backend.admin.dto.response.AdminMajorRateResponse;
import com.sungjiduk.backend.admin.dto.response.AdminMajorUserResponse;
import com.sungjiduk.backend.admin.service.AdminMajorService;
import com.sungjiduk.backend.common.api.ApiResponse;

@RestController
@RequestMapping("/api/admin/major")
public class AdminMajorController {
    private final AdminMajorService adminMajorService;

    public AdminMajorController(AdminMajorService adminMajorService) {
        this.adminMajorService = adminMajorService;
    }

    @GetMapping("/user")
    public ApiResponse<AdminMajorUserResponse> user(AdminMajorUserRateRequest request) {
        return ApiResponse.ok(adminMajorService.user(request));
    }

    @GetMapping("/rate")
    public ApiResponse<AdminMajorRateResponse> rate(AdminMajorUserRateRequest request) {
        return ApiResponse.ok(adminMajorService.rate(request));
    }

    @GetMapping("/content")
    public ApiResponse<AdminMajorContentSpotResponse> content(AdminMajorContentSpotRequest request) {
        return ApiResponse.ok(adminMajorService.content(request));
    }

    @GetMapping("/spot")
    public ApiResponse<AdminMajorContentSpotResponse> spot(AdminMajorContentSpotRequest request) {
        return ApiResponse.ok(adminMajorService.spot(request));
    }
}
