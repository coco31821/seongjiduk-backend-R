package com.sungjiduk.backend.visit.dto.request;

import jakarta.validation.constraints.NotNull;

public record VisitCreateRequest(
        @NotNull Long spotId,
        String memo,
        String photoUrl
) {
}
