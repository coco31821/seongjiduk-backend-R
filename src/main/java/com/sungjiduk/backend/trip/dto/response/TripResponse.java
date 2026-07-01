package com.sungjiduk.backend.trip.dto.response;

import java.util.List;

public record TripResponse(
        Long tripId,
        String title,
        List<DayPlan> days,
        String shareText
) {

    public record DayPlan(
            int dayNo,
            String summary,
            List<Stop> stops
    ) {
    }

    public record Stop(
            int sequence,
            String spotType,
            Long spotId,
            String name,
            String arrivalTime,
            int stayMinutes,
            String reason
    ) {
    }
}
