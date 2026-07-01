package com.sungjiduk.backend.trip.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TripGenerateRequest(
        @NotNull Long contentId,
        @Min(1) int durationDays,
        String budgetLevel,
        String startLocation,
        String travelStyle,
        List<Long> selectedSpotIds,
        List<Long> excludedSpotIds
) {
}
