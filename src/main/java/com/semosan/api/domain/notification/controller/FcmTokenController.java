package com.semosan.api.domain.notification.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.notification.dto.FcmTokenDeleteRequest;
import com.semosan.api.domain.notification.dto.FcmTokenRegisterRequest;
import com.semosan.api.domain.notification.service.FcmTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @PostMapping("/tokens")
    public ResponseEntity<ApiResponse<Void>> register(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FcmTokenRegisterRequest request
    ) {
        fcmTokenService.register(userId, request.token(), request.deviceType());
        return ApiResponse.success(SuccessStatus.FCM_TOKEN_REGISTER_SUCCESS);
    }

    @DeleteMapping("/tokens")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FcmTokenDeleteRequest request
    ) {
        fcmTokenService.delete(userId, request.token());
        return ApiResponse.success(SuccessStatus.FCM_TOKEN_DELETE_SUCCESS);
    }
}
