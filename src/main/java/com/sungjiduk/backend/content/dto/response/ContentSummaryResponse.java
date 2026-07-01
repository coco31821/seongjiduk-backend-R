package com.sungjiduk.backend.content.dto.response;

public record ContentSummaryResponse(
        Long id,
        String title,
        String category,
        String country
) {
}
