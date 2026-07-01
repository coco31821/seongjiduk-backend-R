package com.sungjiduk.backend.spot.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SpotReportCreateRequest(
        Long contentId,
        @NotBlank String spotName,
        @NotBlank String address,
        String referenceUrl,
        String memo
) {
}
