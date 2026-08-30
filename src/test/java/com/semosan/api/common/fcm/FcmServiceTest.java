package com.semosan.api.common.fcm;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.util.List;
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

    @Test
    void sendMessageAllowsEmptyData() throws Exception {
        FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
        when(firebaseMessaging.send(org.mockito.ArgumentMatchers.any(Message.class))).thenReturn("message-id");
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);

        try (MockedStatic<FirebaseMessaging> mockedStatic = mockStatic(FirebaseMessaging.class)) {
            mockedStatic.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);

            String result = fcmService.sendMessage("token", "title", "body", Map.of(), true);

            assertThat(result).isEqualTo("message-id");
            org.mockito.Mockito.verify(firebaseMessaging).send(captor.capture());
        }

        Message message = captor.getValue();
        assertThat(read(message, "getData")).isNull();
        assertThat(read(message, "getNotification")).isNull();
    }

    @Test
    void sendEachForMulticastBuildsNormalPushWithNotificationAndData() throws Exception {
        FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
        BatchResponse batchResponse = mock(BatchResponse.class);
        when(firebaseMessaging.sendEachForMulticast(org.mockito.ArgumentMatchers.any(MulticastMessage.class)))
                .thenReturn(batchResponse);
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);

        try (MockedStatic<FirebaseMessaging> mockedStatic = mockStatic(FirebaseMessaging.class)) {
            mockedStatic.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);

            BatchResponse result = fcmService.sendEachForMulticast(
                    List.of("token-1", "token-2"), "title", "body", Map.of("type", "COMMUNITY_COMMENT"), false);

            assertThat(result).isSameAs(batchResponse);
            org.mockito.Mockito.verify(firebaseMessaging).sendEachForMulticast(captor.capture());
        }

        List<Message> messages = messageList(captor.getValue());
        assertThat(messages).hasSize(2);
        assertThat(read(messages.get(0), "getToken")).isEqualTo("token-1");
        assertThat(read(messages.get(1), "getToken")).isEqualTo("token-2");
        assertThat(read(messages.get(0), "getNotification")).isNotNull();
        assertThat(read(messages.get(0), "getApnsConfig")).isNotNull();
        assertThat(read(messages.get(0), "getData")).isEqualTo(Map.of("type", "COMMUNITY_COMMENT"));
    }

    @Test
    void sendEachForMulticastBuildsDataOnlyPushWithoutNotification() throws Exception {
        FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
        BatchResponse batchResponse = mock(BatchResponse.class);
        when(firebaseMessaging.sendEachForMulticast(org.mockito.ArgumentMatchers.any(MulticastMessage.class)))
                .thenReturn(batchResponse);
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);

        try (MockedStatic<FirebaseMessaging> mockedStatic = mockStatic(FirebaseMessaging.class)) {
            mockedStatic.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);

            fcmService.sendEachForMulticast(List.of("token-1"), "title", "body", Map.of("type", "TRACKING"), true);

            org.mockito.Mockito.verify(firebaseMessaging).sendEachForMulticast(captor.capture());
        }

        Message message = messageList(captor.getValue()).get(0);
        assertThat(read(message, "getNotification")).isNull();
        assertThat(read(message, "getApnsConfig")).isNotNull();
        assertThat(read(message, "getData")).isEqualTo(Map.of("type", "TRACKING"));
    }

    private Object read(Message message, String methodName) throws Exception {
        Method method = Message.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(message);
    }

    @SuppressWarnings("unchecked")
    private List<Message> messageList(MulticastMessage multicastMessage) throws Exception {
        Method method = MulticastMessage.class.getDeclaredMethod("getMessageList");
        method.setAccessible(true);
        return (List<Message>) method.invoke(multicastMessage);
    }
}
