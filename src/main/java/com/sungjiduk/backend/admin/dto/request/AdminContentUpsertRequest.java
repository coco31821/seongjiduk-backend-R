package com.sungjiduk.backend.admin.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminContentUpsertRequest(
        @NotBlank String title,
        @NotBlank String category,
        @NotBlank String country,
        String description
) {
}
