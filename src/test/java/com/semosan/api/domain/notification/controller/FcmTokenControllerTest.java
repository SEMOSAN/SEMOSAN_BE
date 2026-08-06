package com.semosan.api.domain.notification.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.notification.dto.FcmTokenDeleteRequest;
import com.semosan.api.domain.notification.dto.FcmTokenRegisterRequest;
import com.semosan.api.domain.notification.service.FcmTokenService;
import com.semosan.api.domain.user.enums.user.DeviceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FcmTokenControllerTest {

    @Mock
    private FcmTokenService fcmTokenService;

    @InjectMocks
    private FcmTokenController fcmTokenController;

    @Test
    void registerDelegatesAndReturnsSuccessResponse() {
        ResponseEntity<ApiResponse<Void>> response = fcmTokenController.register(
                1L,
                new FcmTokenRegisterRequest("fcm-token", DeviceType.IOS)
        );

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.FCM_TOKEN_REGISTER_SUCCESS.getHttpStatus());
        verify(fcmTokenService).register(1L, "fcm-token", DeviceType.IOS);
    }

    @Test
    void deleteDelegatesAndReturnsSuccessResponse() {
        ResponseEntity<ApiResponse<Void>> response = fcmTokenController.delete(
                1L,
                new FcmTokenDeleteRequest("fcm-token")
        );

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.FCM_TOKEN_DELETE_SUCCESS.getHttpStatus());
        verify(fcmTokenService).delete(1L, "fcm-token");
    }
}
