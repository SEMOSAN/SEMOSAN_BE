package com.semosan.api.domain.notification.dto;

import com.semosan.api.domain.user.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FcmTokenRegisterRequest(
        @NotBlank String token,
        @NotNull DeviceType deviceType
) {
}
