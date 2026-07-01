package com.sungjiduk.backend.spot.dto.response;

public record SpotDetailResponse(
        Long id,
        String name,
        String city,
        String address,
        double lat,
        double lng,
        int recommendedDurationMin,
        String referenceUrl
) {
}
