package com.semosan.api.domain.notification.dispatcher;

/**
 * 알림 발송 추상화. 현재는 @Async 인메모리 처리, 추후 Redis/Kafka 등으로 교체 가능.
 * NotificationService는 이 인터페이스에만 의존한다.
 */
public interface NotificationDispatcher {
    void dispatch(NotificationDispatchCommand command);
}
