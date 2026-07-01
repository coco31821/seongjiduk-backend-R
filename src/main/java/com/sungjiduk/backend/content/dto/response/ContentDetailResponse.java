package com.sungjiduk.backend.content.dto.response;

public record ContentDetailResponse(
        Long id,
        String title,
        String category,
        String country,
        String description
) {
}
