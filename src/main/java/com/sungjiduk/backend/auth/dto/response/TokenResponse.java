package com.sungjiduk.backend.auth.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
