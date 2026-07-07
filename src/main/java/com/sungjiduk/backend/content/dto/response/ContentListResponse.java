package com.sungjiduk.backend.content.dto.response;

public record ContentListResponse(
        Long id,
        String title,
        String category,
        String country
,
        long spotCount) {
}
