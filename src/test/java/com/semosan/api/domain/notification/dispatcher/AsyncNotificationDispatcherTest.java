package com.semosan.api.domain.notification.dispatcher;

import com.semosan.api.common.fcm.FcmService;
import com.semosan.api.domain.notification.enums.NotificationType;
import com.semosan.api.domain.notification.service.FcmTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AsyncNotificationDispatcherTest {

    @Mock
    private FcmService fcmService;

    @Mock
    private FcmTokenService fcmTokenService;

    @Test
    @SuppressWarnings("unchecked")
    void dispatchSendsTrackingNotificationAsDataOnlyWithTitleBodyAndDistance() throws Exception {
        AsyncNotificationDispatcher dispatcher = dispatcher();
        NotificationDispatchCommand command = new NotificationDispatchCommand(
                1L,
                10L,
                NotificationType.TRACKING_PHOTO_MILESTONE,
                "SEMOSAN",
                "500m 돌파! 인증 사진을 남겨보세요!",
                Map.of("distance", 500),
                List.of("token-1")
        );

        dispatcher.dispatch(command);

        ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(fcmService).sendMessage(
                eq("token-1"),
                eq("SEMOSAN"),
                eq("500m 돌파! 인증 사진을 남겨보세요!"),
                dataCaptor.capture(),
                eq(true)
        );
        assertThat(dataCaptor.getValue())
                .containsEntry("type", "TRACKING_PHOTO_MILESTONE")
                .containsEntry("title", "SEMOSAN")
                .containsEntry("body", "500m 돌파! 인증 사진을 남겨보세요!")
                .containsEntry("distance", "500")
                .containsEntry("notificationId", "1")
                .doesNotContainKey("extras");
    }

    @Test
    @SuppressWarnings("unchecked")
    void dispatchKeepsGeneralNotificationPayload() throws Exception {
        AsyncNotificationDispatcher dispatcher = dispatcher();
        NotificationDispatchCommand command = new NotificationDispatchCommand(
                2L,
                10L,
                NotificationType.COMMUNITY_COMMENT,
                "새 댓글이 달렸어요",
                "푸름: 확인했어요",
                Map.of("actorName", "푸름", "commentPreview", "확인했어요"),
                List.of("token-1")
        );

        dispatcher.dispatch(command);

        ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(fcmService).sendMessage(
                eq("token-1"),
                eq("새 댓글이 달렸어요"),
                eq("푸름: 확인했어요"),
                dataCaptor.capture(),
                eq(false)
        );
        assertThat(dataCaptor.getValue())
                .containsEntry("type", "COMMUNITY_COMMENT")
                .containsEntry("title", "새 댓글이 달렸어요")
                .containsEntry("body", "푸름: 확인했어요")
                .doesNotContainKey("extras");
    }

    private AsyncNotificationDispatcher dispatcher() {
        return new AsyncNotificationDispatcher(
                fcmService,
                fcmTokenService,
                new FcmPayloadPolicy()
        );
    }
}
