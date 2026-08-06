package com.semosan.api.domain.notification.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.notification.dto.NotificationTestRequest;
import com.semosan.api.domain.notification.enums.NotificationType;
import com.semosan.api.domain.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationTestControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationTestController notificationTestController;

    @Test
    void sendDelegatesAndReturnsSuccessResponse() {
        NotificationTestRequest request = new NotificationTestRequest(
                1L,
                NotificationType.TRACKING_PHOTO_MILESTONE,
                Map.of("distance", 500)
        );

        ResponseEntity<ApiResponse<Void>> response = notificationTestController.send(request);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.NOTIFICATION_SEND_SUCCESS.getHttpStatus());
        verify(notificationService).send(1L, NotificationType.TRACKING_PHOTO_MILESTONE, Map.of("distance", 500));
    }
}
