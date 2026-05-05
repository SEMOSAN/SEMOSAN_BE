package com.semosan.api.domain.notification.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.notification.dispatcher.NotificationDispatchCommand;
import com.semosan.api.domain.notification.dispatcher.NotificationDispatcher;
import com.semosan.api.domain.notification.entity.FcmToken;
import com.semosan.api.domain.notification.entity.Notification;
import com.semosan.api.domain.notification.enums.NotificationType;
import com.semosan.api.domain.notification.repository.FcmTokenRepository;
import com.semosan.api.domain.notification.repository.NotificationRepository;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationDispatcher dispatcher;
    private final NotificationRepository notificationRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    /**
     * 서비스로 어디서든 한 줄로 알림 발송 가능
     */
    @Transactional
    public void send(Long receiverId, NotificationType type, Map<String, Object> params) {
        // 1. 받는 사람 검증
        if (!userRepository.existsById(receiverId)) {
            throw new GeneralException(ErrorStatus.USER_NOT_FOUND);
        }

        // 2. 파라미터 검증 (NotificationType별 필수 키)
        type.validate(params);

        // 3. 템플릿 처리
        String title = type.formatTitle(params);
        String body = type.formatBody(params);

        // 4. 이력 저장
        Notification notification = notificationRepository.save(
                Notification.create(receiverId, type, title, body, params)
        );

        // 5. 토큰 조회, 없으면 정상 종료 (유저가 알림 거부/로그아웃 등 정상 케이스)
        List<String> tokens = fcmTokenRepository.findAllByUserId(receiverId).stream()
                .map(FcmToken::getToken)
                .toList();

        if (tokens.isEmpty()) {
            log.info("발송 대상 토큰 없음 (userId={}, type={})", receiverId, type);
            return;
        }

        // 6. 비동기 발송 위임
        dispatcher.dispatch(new NotificationDispatchCommand(
                notification.getId(),
                receiverId,
                type,
                title,
                body,
                params,
                tokens
        ));
    }
}
