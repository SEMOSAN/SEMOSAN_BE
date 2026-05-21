package com.semosan.api.domain.auth.dto.response;

import com.semosan.api.common.jwt.TokenIssuance;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.OnboardingStatus;

public record LoginResponse(
        Long userId,
        String accessToken,
        String refreshToken,
        boolean onboardingCompleted
) {

    public static LoginResponse from(User user, TokenIssuance tokens) {
        return new LoginResponse(
                user.getId(),
                tokens.accessToken(),
                tokens.refreshToken(),
                user.getOnboardingStatus() == OnboardingStatus.COMPLETE
        );
    }
}
