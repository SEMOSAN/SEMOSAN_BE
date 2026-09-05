package com.semosan.api.domain.notification.dispatcher;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import com.semosan.api.common.fcm.FcmService;
import com.semosan.api.domain.notification.enums.NotificationTargetType;
import com.semosan.api.domain.notification.service.FcmTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncNotificationDispatcher implements NotificationDispatcher {

    // FCM sendEachForMulticast 1회 호출당 담을 수 있는 최대 토큰 수 (SDK 상한).
    private static final int MAX_BATCH_SIZE = 500;

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
        List<String> tokens = cmd.tokens();

        for (int from = 0; from < tokens.size(); from += MAX_BATCH_SIZE) {
            List<String> chunk = tokens.subList(from, Math.min(from + MAX_BATCH_SIZE, tokens.size()));
            sendChunk(chunk, cmd, dataPayload);
        }
    }

    private void sendChunk(List<String> chunk, NotificationDispatchCommand cmd, Map<String, String> dataPayload) {
        try {
            BatchResponse response = fcmService.sendEachForMulticast(
                    chunk, cmd.title(), cmd.body(), dataPayload, cmd.type().isDataOnly());
            handleBatchResponse(chunk, response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 배치 발송 자체 실패 (tokenCount={}): {}", chunk.size(), e.getMessage());
        } catch (Exception e) {
            log.error("FCM 배치 발송 중 알 수 없는 에러 (tokenCount={}): {}", chunk.size(), e.getMessage(), e);
        }
    }

    // BatchResponse.getResponses() 는 요청한 토큰 리스트와 같은 순서로 오므로 인덱스로 매칭한다.
    private void handleBatchResponse(List<String> chunk, BatchResponse response) {
        List<SendResponse> results = response.getResponses();
        for (int i = 0; i < results.size(); i++) {
            SendResponse result = results.get(i);
            if (!result.isSuccessful()) {
                handleSendError(chunk.get(i), result.getException());
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

        // 알림함 응답과 동일한 라우팅 계약을 푸시에도 실어, 앱이 두 경로를 같은 로직으로 처리하게 한다.
        Long targetId = cmd.type().resolveTargetId(cmd.extras());
        NotificationTargetType targetType =
                targetId == null ? NotificationTargetType.NONE : cmd.type().getTargetType();
        data.put("targetType", targetType.name());
        if (targetId != null) {
            data.put("targetId", String.valueOf(targetId));
        }
        return data;
    }

    private void handleSendError(String token, FirebaseMessagingException e) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
            log.warn("만료/잘못된 토큰 삭제 (token={}, code={})", maskToken(token), code);
            fcmTokenService.deleteExpired(token);
        } else {
            log.error("FCM 발송 실패 (token={}, code={}): {}", maskToken(token), code, e.getMessage());
        }
    }

    private String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "<empty>";
        }
        int visibleLength = Math.min(8, token.length());
        return token.substring(0, visibleLength) + "***";
    }
}
