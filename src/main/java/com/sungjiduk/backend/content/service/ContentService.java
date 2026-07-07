package com.sungjiduk.backend.content.service;

import com.sungjiduk.backend.content.dto.response.ContentDetailResponse;
import com.sungjiduk.backend.content.dto.response.ContentSpotsResponse;
import com.sungjiduk.backend.content.dto.response.ContentListResponse;
import com.sungjiduk.backend.content.repository.ContentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContentService {

    private final ContentRepository contentRepository;

    public ContentService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    public List<ContentListResponse> findContents(String category, String country) {
        return contentRepository.findContents(category,country).stream()
            .map(content -> new ContentListResponse(
                content.getId(),
                content.getTitle(),
                content.getCategory(),
                content.getCountry()
            )).toList();
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
