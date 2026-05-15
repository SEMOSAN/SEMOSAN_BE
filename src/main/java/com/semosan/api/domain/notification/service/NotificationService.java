package com.semosan.api.domain.notification.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.notification.dispatcher.NotificationDispatchCommand;
import com.semosan.api.domain.notification.entity.FcmToken;
import com.semosan.api.domain.notification.entity.Notification;
import com.semosan.api.domain.notification.enums.NotificationType;
import com.semosan.api.domain.notification.event.NotificationCreatedEvent;
import com.semosan.api.domain.notification.repository.FcmTokenRepository;
import com.semosan.api.domain.notification.repository.NotificationRepository;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 서비스로 어디서든 한 줄로 알림 발송 가능
     *
     * 흐름:
     *   1. 받는 사람 / 파라미터 검증
     *   2. 템플릿 처리 → title/body
     *   3. Notification 이력 저장
     *   4. 받는 사람 토큰 조회 (없으면 정상 종료)
     *   5. NotificationCreatedEvent 발행
     *      → NotificationEventListener가 트랜잭션 커밋 후에 dispatcher.dispatch() 호출
     *      → 롤백 시 푸시도 안 보내짐 (DB-푸시 일관성)
     */
    @Transactional
    public void send(Long receiverId, NotificationType type, Map<String, Object> params) {
        if (!userRepository.existsByIdAndDeletedFalse(receiverId)) {
            throw new GeneralException(ErrorStatus.USER_NOT_FOUND);
        }

        type.validate(params);

        String title = type.formatTitle(params);
        String body = type.formatBody(params);

        Notification notification = notificationRepository.save(
                Notification.create(receiverId, type, title, body, params)
        );

        List<String> tokens = fcmTokenRepository.findAllByUserId(receiverId).stream()
                .map(FcmToken::getToken)
                .toList();

        if (tokens.isEmpty()) {
            log.info("발송 대상 토큰 없음 (userId={}, type={})", receiverId, type);
            return;
        }

        // 트랜잭션 커밋 후에만 실제 발송이 일어나도록 이벤트 발행
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                new NotificationDispatchCommand(
                        notification.getId(),
                        receiverId,
                        type,
                        title,
                        body,
                        params,
                        tokens
                )
        ));
    }
}
