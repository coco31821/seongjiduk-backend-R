package com.sungjiduk.backend.user.dto;

import lombok.Builder;

@Builder
public record TokenBody(
        String email
) {
}
