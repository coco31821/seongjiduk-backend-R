package com.sungjiduk.backend.auth.dto.response;

import com.sungjiduk.backend.user.entity.User;
import com.sungjiduk.backend.user.entity.UserPreference;

public record MeResponse(
    Long id,
    String email,
    String nickname,
    String travel_style,
    String budget_level
) {


    public static MeResponse from(User user, UserPreference userPreference) {
        return new MeResponse(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            userPreference == null ? null : userPreference.getTravelStyle(),
            userPreference == null ? null : userPreference.getBudgetLevel()
        );
    }
}
