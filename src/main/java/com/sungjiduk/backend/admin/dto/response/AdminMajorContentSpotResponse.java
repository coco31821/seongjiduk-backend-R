package com.sungjiduk.backend.admin.dto.response;

import java.util.List;

public record AdminMajorContentSpotResponse(
    List<List<info>> values,
    List<String> dates
) {
    public record info(
        String name,
        Long count,
        Double rate
    ) { }
}
