package com.sungjiduk.backend.admin.dto.response;

import java.util.List;

public record StatsSeriesResponse(
        String metric,
        List<Point> points
) {

    public record Point(
            String label,
            String value
    ) {
    }
}
