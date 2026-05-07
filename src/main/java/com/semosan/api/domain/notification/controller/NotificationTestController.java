package com.semosan.api.domain.notification.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.notification.dto.NotificationTestRequest;
import com.semosan.api.domain.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림 테스트용 컨트롤러. local 프로필에서만 활성화 (운영 배포 시 자동 비활성화).
 */
@Profile("local")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationTestController {

    private final NotificationService notificationService;

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<Void>> send(
            @Valid @RequestBody NotificationTestRequest request
    ) {
        notificationService.send(request.receiverId(), request.type(), request.params());
        return ApiResponse.success(SuccessStatus.NOTIFICATION_SEND_SUCCESS);
    }
}
