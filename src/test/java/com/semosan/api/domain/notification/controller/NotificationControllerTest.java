package com.semosan.api.domain.notification.controller;

import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.notification.dto.response.NotificationResponse;
import com.semosan.api.domain.notification.enums.NotificationTargetType;
import com.semosan.api.domain.notification.enums.NotificationType;
import com.semosan.api.domain.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @Test
    void getNotificationsReturnsPagedResponses() {
        NotificationResponse response = response();
        PageRequest pageable = PageRequest.of(0, 20);
        when(notificationService.getNotifications(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(response), pageable, 1));

        var body = notificationController.getNotifications(1L, pageable).getBody();

        assertThat(body.getData().content()).containsExactly(response);
        assertThat(body.getData().totalElements()).isEqualTo(1);
    }

    @Test
    void getUnreadCountReturnsCount() {
        when(notificationService.getUnreadCount(1L)).thenReturn(3L);

        assertThat(notificationController.getUnreadCount(1L).getBody().getData()).isEqualTo(3L);
    }

    @Test
    void markAsReadDelegatesToService() {
        assertThat(notificationController.markAsRead(1L, 10L).getStatusCode())
                .isEqualTo(SuccessStatus.NOTIFICATION_READ_SUCCESS.getHttpStatus());

        verify(notificationService).markAsRead(1L, 10L);
    }

    @Test
    void markAllAsReadDelegatesToService() {
        assertThat(notificationController.markAllAsRead(1L).getStatusCode())
                .isEqualTo(SuccessStatus.NOTIFICATION_READ_ALL_SUCCESS.getHttpStatus());

        verify(notificationService).markAllAsRead(1L);
    }

    private NotificationResponse response() {
        return new NotificationResponse(
                10L,
                NotificationType.SEMOFEED_EMOJI,
                "세모피드에 반응이 달렸어요",
                "푸름님이 세모피드에 🔥 반응을 남겼어요",
                NotificationTargetType.SEMOFEED,
                42L,
                false,
                LocalDateTime.of(2026, 9, 5, 14, 20)
        );
    }
}
