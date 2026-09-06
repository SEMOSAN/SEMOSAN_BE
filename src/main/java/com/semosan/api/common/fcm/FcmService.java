package com.semosan.api.common.fcm;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
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
import java.util.Optional;

@Slf4j
@Service
public class FcmService {

    /**
     * @param badge 앱 아이콘에 표시할 안읽음 수. null 이면 설정하지 않는다.
     *              앱이 종료된 상태에서는 클라이언트가 뱃지를 갱신할 수 없어 서버가 실어 보내야 한다.
     */
    public String sendMessage(
            String token,
            String title,
            String body,
            Map<String, String> data,
            boolean dataOnly,
            Integer badge
    ) throws FirebaseMessagingException {
        Message.Builder builder = Message.builder()
                .setToken(token);

        if (!dataOnly) {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();
            builder.setNotification(notification);
            androidBadgeConfig(badge).ifPresent(builder::setAndroidConfig);
        }

        builder.setApnsConfig(dataOnly ? silentPushApnsConfig(badge) : normalPushApnsConfig(title, body, badge));

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
            boolean dataOnly,
            Integer badge
    ) throws FirebaseMessagingException {
        MulticastMessage.Builder builder = MulticastMessage.builder()
                .addAllTokens(tokens);

        if (!dataOnly) {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();
            builder.setNotification(notification);
            androidBadgeConfig(badge).ifPresent(builder::setAndroidConfig);
        }

        builder.setApnsConfig(dataOnly ? silentPushApnsConfig(badge) : normalPushApnsConfig(title, body, badge));

        if (data != null && !data.isEmpty()) {
            builder.putAllData(data);
        }

        BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(builder.build());
        log.info("FCM 배치 발송 완료: 성공={} 실패={}", response.getSuccessCount(), response.getFailureCount());
        return response;
    }

    private ApnsConfig normalPushApnsConfig(String title, String body, Integer badge) {
        Aps.Builder aps = Aps.builder()
                .setAlert(ApsAlert.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setSound("default")
                .setContentAvailable(true);
        if (badge != null) {
            aps.setBadge(badge);
        }
        return ApnsConfig.builder()
                .putHeader("apns-push-type", "alert")
                .putHeader("apns-priority", "10")
                .setAps(aps.build())
                .build();
    }

    private ApnsConfig silentPushApnsConfig(Integer badge) {
        Aps.Builder aps = Aps.builder()
                .setContentAvailable(true);
        if (badge != null) {
            aps.setBadge(badge);
        }
        return ApnsConfig.builder()
                .putHeader("apns-push-type", "background")
                .putHeader("apns-priority", "5")
                .setAps(aps.build())
                .build();
    }

    /**
     * Android 뱃지. 런처마다 표시 여부가 달라 보장되지는 않지만, 지원하는 런처에서는 반영된다.
     * 여기서 title/body 는 지정하지 않아 상위 notification 값이 그대로 쓰인다.
     */
    private Optional<AndroidConfig> androidBadgeConfig(Integer badge) {
        if (badge == null) {
            return Optional.empty();
        }
        return Optional.of(AndroidConfig.builder()
                .setNotification(AndroidNotification.builder()
                        .setNotificationCount(badge)
                        .build())
                .build());
    }

}
