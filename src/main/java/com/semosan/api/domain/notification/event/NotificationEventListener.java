package com.semosan.api.domain.notification.event;

import com.semosan.api.domain.notification.dispatcher.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationDispatcher dispatcher;

    /**
     * 알림 이력 저장 트랜잭션이 커밋된 직후에만 실행
     * 롤백되면 호출 자체가 일어나지 않아 푸시도 안 보내짐 → DB와 푸시 상태 일관성 보장.
     *
     * dispatcher 자체가 @Async라 호출 즉시 리턴하므로 커밋 후 응답 지연도 거의 없음.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        dispatcher.dispatch(event.command());
    }
}
