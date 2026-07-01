package com.sungjiduk.backend.content.dto.response;

import java.util.List;

public record ContentSpotsResponse(
        Long contentId,
        String contentTitle,
        List<SpotSummary> spots
) {

    public record SpotSummary(
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
}
