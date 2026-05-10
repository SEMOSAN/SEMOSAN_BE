package com.semosan.api.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdatePushNotificationSettingRequest(
        @NotNull
        Boolean enabled
) {
}
