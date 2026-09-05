package com.semosan.api.domain.notification.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.notification.controller.docs.NotificationControllerDocs;
import com.semosan.api.domain.notification.dto.response.NotificationResponse;
import com.semosan.api.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerDocs {

    private final NotificationService notificationService;

    @GetMapping
    @Override
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<NotificationResponse> response = PageResponse.from(
                notificationService.getNotifications(userId, pageable)
        );
        return ApiResponse.success(SuccessStatus.NOTIFICATION_LIST_SUCCESS, response);
    }

    @GetMapping("/unread-count")
    @Override
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal Long userId
    ) {
        long count = notificationService.getUnreadCount(userId);
        return ApiResponse.success(SuccessStatus.NOTIFICATION_UNREAD_COUNT_SUCCESS, count);
    }

    @PatchMapping("/{notificationId}/read")
    @Override
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long notificationId
    ) {
        notificationService.markAsRead(userId, notificationId);
        return ApiResponse.success(SuccessStatus.NOTIFICATION_READ_SUCCESS);
    }

    @PatchMapping("/read-all")
    @Override
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal Long userId
    ) {
        notificationService.markAllAsRead(userId);
        return ApiResponse.success(SuccessStatus.NOTIFICATION_READ_ALL_SUCCESS);
    }
}
