package com.semosan.api.domain.notification.dispatcher;

import com.semosan.api.domain.notification.enums.NotificationType;

import java.util.List;
import java.util.Map;

/**
 * 발송 일감. 불변(record)이라 비동기/직렬화 안전.
 * 카프카나 레디스 큐로 갈아탈 때 그대로 메시지 페이로드로 사용 가능.
 */
public record NotificationDispatchCommand(
        Long notificationId,
        Long receiverId,
        NotificationType type,
        String title,
        String body,
        Map<String, Object> extras,
        List<String> tokens
) {
}
