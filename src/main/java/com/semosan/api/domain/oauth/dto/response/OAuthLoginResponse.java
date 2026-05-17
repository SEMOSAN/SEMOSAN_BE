package com.semosan.api.domain.oauth.dto.response;

import com.semosan.api.common.jwt.TokenIssuance;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.OnboardingStatus;

public record OAuthLoginResponse(
        Long userId,
        String accessToken,
        String refreshToken,
        boolean onboardingCompleted
) {

    public static OAuthLoginResponse from(User user, TokenIssuance tokens) {
        return new OAuthLoginResponse(
                user.getId(),
                tokens.accessToken(),
                tokens.refreshToken(),
                user.getOnboardingStatus() == OnboardingStatus.COMPLETE
        );
    }
}
