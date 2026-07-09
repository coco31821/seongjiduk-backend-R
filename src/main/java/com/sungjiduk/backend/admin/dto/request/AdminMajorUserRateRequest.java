package com.sungjiduk.backend.admin.dto.request;

import java.time.LocalDate;

public record AdminMajorUserRateRequest(
    Duration duration,
    LocalDate date
) {
}
