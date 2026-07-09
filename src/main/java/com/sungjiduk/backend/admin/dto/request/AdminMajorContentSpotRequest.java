package com.sungjiduk.backend.admin.dto.request;

import java.time.LocalDate;

public record AdminMajorContentSpotRequest(
    Duration duration,
    LocalDate date,
    Long count
) {
}
