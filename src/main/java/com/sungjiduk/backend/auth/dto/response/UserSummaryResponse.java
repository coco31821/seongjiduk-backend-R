package com.sungjiduk.backend.auth.dto.response;

import com.sungjiduk.backend.user.entity.User;

public record UserSummaryResponse(
        Long id,
        String email,
        String nickname
//        String role
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
            user.getId(), user.getEmail(), user.getNickname()
        );
    }
}
