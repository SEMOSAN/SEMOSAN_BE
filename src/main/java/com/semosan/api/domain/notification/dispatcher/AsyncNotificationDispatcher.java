package com.semosan.api.domain.notification.dispatcher;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.semosan.api.common.fcm.FcmService;
import com.semosan.api.domain.notification.service.FcmTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncNotificationDispatcher implements NotificationDispatcher {

    private final FcmService fcmService;
    private final FcmTokenService fcmTokenService;

    /**
     * @Async 가 같은 클래스에 있는 함수를 호출 할 때는 비동기로 작동하지 않으니 주의해야함
     * AOP 프록시를 사용해서 그렇고, 그래서 빈으로 등록된 다른 클래스에서 호출해야함
     */
    @Override
    @Async("notificationTaskExecutor")
    public void dispatch(NotificationDispatchCommand cmd) {
        Map<String, String> dataPayload = buildDataPayload(cmd);

        for (String token : cmd.tokens()) {
            try {
                fcmService.sendMessage(token, cmd.title(), cmd.body(), dataPayload, cmd.type().isDataOnly());
            } catch (FirebaseMessagingException e) {
                handleSendError(token, e);
            } catch (Exception e) {
                log.error("FCM 발송 중 알 수 없는 에러 (token={}): {}", token, e.getMessage(), e);
            }
        }
    }

    private Map<String, String> buildDataPayload(NotificationDispatchCommand cmd) {
        Map<String, String> data = new HashMap<>();
        if (cmd.extras() != null) {
            cmd.extras().forEach((key, value) -> {
                if (key != null && value != null) {
                    data.put(key, String.valueOf(value));
                }
            });
        }
        data.put("type", cmd.type().name());
        data.put("title", cmd.title());
        data.put("body", cmd.body());
        data.put("notificationId", String.valueOf(cmd.notificationId()));
        return data;
    }

    private void handleSendError(String token, FirebaseMessagingException e) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
            log.warn("만료/잘못된 토큰 삭제 (token={}, code={})", token, code);
            fcmTokenService.deleteExpired(token);
        } else {
            log.error("FCM 발송 실패 (token={}, code={}): {}", token, code, e.getMessage());
        }
    }
}
