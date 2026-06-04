package com.semosan.api.common.fcm;

import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApsAlert;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class FcmService {

    @Value("${fcm.apns-environment:production}")
    private String apnsEnvironment;

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

    private ApnsConfig normalPushApnsConfig(String title, String body) {
        return ApnsConfig.builder()
                .putHeader("apns-environment", apnsEnvironment)
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
                .putHeader("apns-environment", apnsEnvironment)
                .setAps(Aps.builder()
                        .setContentAvailable(true)
                        .build())
                .build();
    }

}
