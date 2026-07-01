package com.sungjiduk.backend.content.service;

import com.sungjiduk.backend.content.dto.response.ContentDetailResponse;
import com.sungjiduk.backend.content.dto.response.ContentSpotsResponse;
import com.sungjiduk.backend.content.dto.response.ContentSummaryResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContentService {

    public List<ContentSummaryResponse> findContents() {
        return List.of(new ContentSummaryResponse(1L, "러브라이브! 뮤즈", "ANIME", "JP"));
    }

    public ContentDetailResponse findContent(Long contentId) {
        return new ContentDetailResponse(contentId, "러브라이브! 뮤즈", "ANIME", "JP", "도쿄 아키하바라 중심 성지순례 콘텐츠");
    }

    public ContentSpotsResponse findContentSpots(Long contentId) {
        return new ContentSpotsResponse(
                contentId,
                "러브라이브! 뮤즈",
                List.of(new ContentSpotsResponse.SpotSummary(
                        1L,
                        "쇼헤이바시",
                        "Tokyo",
                        "Tokyo, Japan",
                        35.697,
                        139.771,
                        30,
                        "https://example.com/reference"
                ))
        );
    }
}
