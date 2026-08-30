package com.semosan.api.common.fcm;

import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApsAlert;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FcmService {

    public String sendMessage(
            String token,
            String title,
            String body,
            Map<String, String> data,
            boolean dataOnly
    ) throws FirebaseMessagingException {
        Message.Builder builder = Message.builder()
                .setToken(token);

        if (!dataOnly) {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();
            builder.setNotification(notification);
        }

        builder.setApnsConfig(dataOnly ? silentPushApnsConfig() : normalPushApnsConfig(title, body));

        if (data != null && !data.isEmpty()) {
            builder.putAllData(data);
        }

        String response = FirebaseMessaging.getInstance().send(builder.build());
        log.info("FCM 발송 성공: {}", response);
        return response;
    }

    /**
     * 토큰 최대 500개까지 한 번에 발송한다 (SDK 상한, 호출부에서 청크 분할 책임).
     * sendEach 기반이라 실제 HTTP 호출은 토큰당 1번씩 나가지만, 순차 blocking 대신
     * SDK가 백그라운드 스레드에서 동시에 처리해 전체 소요시간이 크게 줄어든다.
     */
    public BatchResponse sendEachForMulticast(
            List<String> tokens,
            String title,
            String body,
            Map<String, String> data,
            boolean dataOnly
    ) throws FirebaseMessagingException {
        MulticastMessage.Builder builder = MulticastMessage.builder()
                .addAllTokens(tokens);

        if (!dataOnly) {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();
            builder.setNotification(notification);
        }

        builder.setApnsConfig(dataOnly ? silentPushApnsConfig() : normalPushApnsConfig(title, body));

        if (data != null && !data.isEmpty()) {
            builder.putAllData(data);
        }

        BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(builder.build());
        log.info("FCM 배치 발송 완료: 성공={} 실패={}", response.getSuccessCount(), response.getFailureCount());
        return response;
    }

    private ApnsConfig normalPushApnsConfig(String title, String body) {
        return ApnsConfig.builder()
                .putHeader("apns-push-type", "alert")
                .putHeader("apns-priority", "10")
                .setAps(Aps.builder()
                        .setAlert(ApsAlert.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .setSound("default")
                        .setContentAvailable(true)
                        .build())
                .build();
    }

    private ApnsConfig silentPushApnsConfig() {
        return ApnsConfig.builder()
                .putHeader("apns-push-type", "background")
                .putHeader("apns-priority", "5")
                .setAps(Aps.builder()
                        .setContentAvailable(true)
                        .build())
                .build();
    }

}
