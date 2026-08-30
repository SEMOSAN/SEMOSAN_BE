package com.semosan.api.domain.user.event;

import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.enums.user.OAuthProvider;

import java.time.LocalDateTime;

public record UserRegisteredEvent(
        Long userId,
        String nickname,
        OAuthProvider provider,
        DeviceType deviceType,
        LocalDateTime registeredAt
) {
}
