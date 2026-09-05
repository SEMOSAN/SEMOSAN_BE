package com.semosan.api.domain.notification.dto.response;

import com.semosan.api.domain.notification.entity.Notification;
import com.semosan.api.domain.notification.enums.NotificationTargetType;
import com.semosan.api.domain.notification.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        String title,
        String body,
        NotificationTargetType targetType,
        Long targetId,
        boolean isRead,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        NotificationType type = notification.getType();
        Long targetId = type.resolveTargetId(notification.getExtras());
        // 대상 ID 를 못 뽑으면 앱이 빈 화면으로 이동하지 않도록 이동 불가로 내린다.
        NotificationTargetType targetType =
                targetId == null ? NotificationTargetType.NONE : type.getTargetType();

        return new NotificationResponse(
                notification.getId(),
                type,
                notification.getTitle(),
                notification.getBody(),
                targetType,
                targetId,
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
