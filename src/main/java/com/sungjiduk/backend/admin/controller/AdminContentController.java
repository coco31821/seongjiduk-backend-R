package com.sungjiduk.backend.admin.controller;

import com.sungjiduk.backend.admin.dto.request.AdminContentUpsertRequest;
import com.sungjiduk.backend.admin.dto.response.AdminCommandResponse;
import com.sungjiduk.backend.admin.service.AdminContentService;
import com.sungjiduk.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/contents")
public class AdminContentController {

    private final AdminContentService adminContentService;

    public AdminContentController(AdminContentService adminContentService) {
        this.adminContentService = adminContentService;
    }

    @PostMapping
    public ApiResponse<AdminCommandResponse> create(@Valid @RequestBody AdminContentUpsertRequest request) {


        
        return ApiResponse.ok(adminContentService.create(request));
    }

    @PatchMapping("/{contentId}")
    public ApiResponse<AdminCommandResponse> update(@PathVariable Long contentId, @Valid @RequestBody AdminContentUpsertRequest request) {
        return ApiResponse.ok(adminContentService.update(contentId, request));
    }

    @DeleteMapping("/{contentId}")
    public ApiResponse<AdminCommandResponse> delete(@PathVariable Long contentId) {
        return ApiResponse.ok(adminContentService.delete(contentId));
    }
}
