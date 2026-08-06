package com.semosan.api.common.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class FcmServiceTest {

    private final FcmService fcmService = new FcmService();

    @Test
    void sendMessageBuildsNormalPushWithNotificationAndData() throws Exception {
        FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
        when(firebaseMessaging.send(org.mockito.ArgumentMatchers.any(Message.class))).thenReturn("message-id");
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);

        try (MockedStatic<FirebaseMessaging> mockedStatic = mockStatic(FirebaseMessaging.class)) {
            mockedStatic.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);

            String result = fcmService.sendMessage(
                    "token",
                    "title",
                    "body",
                    Map.of("type", "COMMUNITY_COMMENT"),
                    false
            );

            assertThat(result).isEqualTo("message-id");
            org.mockito.Mockito.verify(firebaseMessaging).send(captor.capture());
        }

        Message message = captor.getValue();
        assertThat(read(message, "getNotification")).isNotNull();
        assertThat(read(message, "getApnsConfig")).isNotNull();
        assertThat(read(message, "getData")).isEqualTo(Map.of("type", "COMMUNITY_COMMENT"));
        assertThat(read(message, "getToken")).isEqualTo("token");
    }

    @Test
    void sendMessageBuildsDataOnlyPushWithoutNotification() throws Exception {
        FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
        when(firebaseMessaging.send(org.mockito.ArgumentMatchers.any(Message.class))).thenReturn("message-id");
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);

        try (MockedStatic<FirebaseMessaging> mockedStatic = mockStatic(FirebaseMessaging.class)) {
            mockedStatic.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);

            String result = fcmService.sendMessage(
                    "token",
                    "title",
                    "body",
                    Map.of("type", "TRACKING"),
                    true
            );

            assertThat(result).isEqualTo("message-id");
            org.mockito.Mockito.verify(firebaseMessaging).send(captor.capture());
        }

        Message message = captor.getValue();
        assertThat(read(message, "getNotification")).isNull();
        assertThat(read(message, "getApnsConfig")).isNotNull();
        assertThat(read(message, "getData")).isEqualTo(Map.of("type", "TRACKING"));
    }

    @Test
    void sendMessageAllowsNullData() throws Exception {
        FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
        when(firebaseMessaging.send(org.mockito.ArgumentMatchers.any(Message.class))).thenReturn("message-id");
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);

        try (MockedStatic<FirebaseMessaging> mockedStatic = mockStatic(FirebaseMessaging.class)) {
            mockedStatic.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);

            String result = fcmService.sendMessage("token", "title", "body", null, false);

            assertThat(result).isEqualTo("message-id");
            org.mockito.Mockito.verify(firebaseMessaging).send(captor.capture());
        }

        Message message = captor.getValue();
        assertThat(read(message, "getData")).isNull();
        assertThat(read(message, "getNotification")).isNotNull();
    }

    private Object read(Message message, String methodName) throws Exception {
        Method method = Message.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(message);
    }
}
