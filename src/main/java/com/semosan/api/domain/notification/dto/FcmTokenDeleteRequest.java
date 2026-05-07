package com.semosan.api.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenDeleteRequest(
        @NotBlank String token
) {
}
