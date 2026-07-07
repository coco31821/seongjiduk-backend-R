package com.sungjiduk.backend.admin.dto.request;

import jakarta.validation.constraints.Positive;

public record SpotImportRequest(
        @Positive long bangumiId
) {
}
