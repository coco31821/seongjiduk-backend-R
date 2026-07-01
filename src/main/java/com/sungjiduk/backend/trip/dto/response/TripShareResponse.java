package com.sungjiduk.backend.trip.dto.response;

public record TripShareResponse(
        Long tripId,
        String shareUrl,
        String shareText
) {
}
