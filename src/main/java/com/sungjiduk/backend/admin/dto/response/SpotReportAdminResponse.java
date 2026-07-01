package com.sungjiduk.backend.admin.dto.response;

public record SpotReportAdminResponse(
        Long reportId,
        String spotName,
        String address,
        String status
) {
}
