package com.sungjiduk.backend.content.controller;

import com.sungjiduk.backend.common.api.ApiResponse;
import com.sungjiduk.backend.content.dto.response.ContentDetailResponse;
import com.sungjiduk.backend.content.dto.response.ContentSpotsResponse;
import com.sungjiduk.backend.content.dto.response.ContentSummaryResponse;
import com.sungjiduk.backend.content.service.ContentService;
import com.sungjiduk.backend.spot.dto.response.RouteVerificationResponse;
import com.sungjiduk.backend.spot.service.RouteVerificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contents")
public class ContentController {

    private final ContentService contentService;
    private final RouteVerificationService routeVerificationService;

    public ContentController(ContentService contentService, RouteVerificationService routeVerificationService) {
        this.contentService = contentService;
        this.routeVerificationService = routeVerificationService;
    }

    @GetMapping
    public ApiResponse<List<ContentSummaryResponse>> contents() {
        return ApiResponse.ok(contentService.findContents());
    }

    @GetMapping("/{contentId}")
    public ApiResponse<ContentDetailResponse> content(@PathVariable Long contentId) {
        return ApiResponse.ok(contentService.findContent(contentId));
    }

    @GetMapping("/{contentId}/spots")
    public ApiResponse<ContentSpotsResponse> contentSpots(@PathVariable Long contentId) {
        return ApiResponse.ok(contentService.findContentSpots(contentId));
    }

    /** 블로그 후기 기반 동선 검증 (네이버 키 없으면 available=false) — 첫 호출은 수집·추출로 수십 초 */
    @GetMapping("/{contentId}/route-verification")
    public ApiResponse<RouteVerificationResponse> routeVerification(@PathVariable Long contentId) {
        return ApiResponse.ok(routeVerificationService.verify(contentId));
    }
}
