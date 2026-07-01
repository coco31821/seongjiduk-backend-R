package com.sungjiduk.backend.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminSpotUpsertRequest(
        @NotNull Long contentId,
        @NotBlank String name,
        @NotBlank String city,
        @NotBlank String address,
        double lat,
        double lng,
        int recommendedDurationMin,
        String referenceUrl
) {
}
