package com.sungjiduk.backend.auth.dto.response;

public record UserSummaryResponse(
        Long id,
        String email,
        String nickname
) {
}
