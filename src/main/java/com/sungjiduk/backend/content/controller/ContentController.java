package com.sungjiduk.backend.content.controller;

import com.sungjiduk.backend.common.api.ApiResponse;
import com.sungjiduk.backend.content.dto.response.ContentDetailResponse;
import com.sungjiduk.backend.content.dto.response.ContentSpotsResponse;
import com.sungjiduk.backend.content.dto.response.ContentListResponse;
import com.sungjiduk.backend.content.service.ContentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contents")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    public ApiResponse<List<ContentListResponse>> contents(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String country
    ) {
        return ApiResponse.ok(contentService.findContents(category,country));
    }

    @GetMapping("/{contentId}")
    public ApiResponse<ContentDetailResponse> content(@PathVariable Long contentId) {
        return ApiResponse.ok(contentService.findContent(contentId));
    }

    @GetMapping("/{contentId}/spots")
    public ApiResponse<ContentSpotsResponse> contentSpots(@PathVariable Long contentId) {
        return ApiResponse.ok(contentService.findContentSpots(contentId));
    }
}
