package com.semosan.api.domain.notification.dto;

import com.semosan.api.domain.notification.enums.NotificationType;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record NotificationTestRequest(
        @NotNull Long receiverId,
        @NotNull NotificationType type,
        Map<String, Object> params
) {
}
