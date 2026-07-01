package com.sungjiduk.backend.trip.dto.response;

public record TripSummaryResponse(
        Long tripId,
        String title,
        int durationDays,
        String status
) {
}
