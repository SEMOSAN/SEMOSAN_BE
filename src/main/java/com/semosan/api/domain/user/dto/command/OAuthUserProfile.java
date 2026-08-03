package com.semosan.api.domain.user.dto.command;

public record OAuthUserProfile(
        String oauthId,
        String email,
        String name
) {
}
