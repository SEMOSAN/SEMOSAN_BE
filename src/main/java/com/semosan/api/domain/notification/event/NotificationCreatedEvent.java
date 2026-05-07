package com.semosan.api.domain.notification.event;

import com.semosan.api.domain.notification.dispatcher.NotificationDispatchCommand;

/**
 * 알림 이력이 DB에 저장된 후 발행되는 도메인 이벤트
 * 이 이벤트는 NotificationEventListener가 트랜잭션 커밋 후(AFTER_COMMIT)에만 처리하므로,
 * 알림 이력 저장과 실제 FCM 발송의 일관성이 보장된다 (롤백 시 푸시도 안 감)
 */
public record NotificationCreatedEvent(NotificationDispatchCommand command) {
}
