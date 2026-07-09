package com.sungjiduk.backend.admin.dto.response;

import java.util.List;

public record AdminMajorRateResponse(
    List<Double> values,
    List<String> dates
) {
}
