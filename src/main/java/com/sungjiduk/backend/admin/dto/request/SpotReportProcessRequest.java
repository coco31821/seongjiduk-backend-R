package com.sungjiduk.backend.admin.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SpotReportProcessRequest(
        @NotBlank String status,
        String memo
) {
}
