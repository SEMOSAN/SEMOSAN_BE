package com.semosan.api.domain.tracking.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.tracking.controller.docs.TrackingSessionControllerDocs;
import com.semosan.api.domain.tracking.dto.request.CreateTrackingSessionRequest;
import com.semosan.api.domain.tracking.dto.response.TrackingSessionResponse;
import com.semosan.api.domain.tracking.service.TrackingSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tracking/sessions")
@RequiredArgsConstructor
public class TrackingSessionController implements TrackingSessionControllerDocs {

    private final TrackingSessionService trackingSessionService;

    @PostMapping
    @Override
    public ResponseEntity<ApiResponse<TrackingSessionResponse>> createSession(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateTrackingSessionRequest request
    ) {
        TrackingSessionResponse response = trackingSessionService.create(userId, request);
        return ApiResponse.success(SuccessStatus.TRACKING_SESSION_CREATE_SUCCESS, response);
    }

    /**
     * 앱 재진입 시 호출. 진행 중 세션이 없으면 data 가 비어 응답된다 (NON_NULL 직렬화).
     */
    @GetMapping("/me/active")
    @Override
    public ResponseEntity<ApiResponse<TrackingSessionResponse>> getActiveSession(
            @AuthenticationPrincipal Long userId
    ) {
        TrackingSessionResponse response = trackingSessionService.getActive(userId).orElse(null);
        return ApiResponse.success(SuccessStatus.TRACKING_SESSION_GET_ACTIVE_SUCCESS, response);
    }

    @GetMapping("/{sessionId}")
    @Override
    public ResponseEntity<ApiResponse<TrackingSessionResponse>> getSession(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId
    ) {
        TrackingSessionResponse response = trackingSessionService.get(userId, sessionId);
        return ApiResponse.success(SuccessStatus.TRACKING_SESSION_GET_SUCCESS, response);
    }

    @PostMapping("/{sessionId}/pause")
    @Override
    public ResponseEntity<ApiResponse<TrackingSessionResponse>> pauseSession(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId
    ) {
        TrackingSessionResponse response = trackingSessionService.pause(userId, sessionId);
        return ApiResponse.success(SuccessStatus.TRACKING_SESSION_PAUSE_SUCCESS, response);
    }

    @PostMapping("/{sessionId}/resume")
    @Override
    public ResponseEntity<ApiResponse<TrackingSessionResponse>> resumeSession(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId
    ) {
        TrackingSessionResponse response = trackingSessionService.resume(userId, sessionId);
        return ApiResponse.success(SuccessStatus.TRACKING_SESSION_RESUME_SUCCESS, response);
    }

    @PostMapping("/{sessionId}/complete")
    @Override
    public ResponseEntity<ApiResponse<TrackingSessionResponse>> completeSession(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId
    ) {
        TrackingSessionResponse response = trackingSessionService.complete(userId, sessionId);
        return ApiResponse.success(SuccessStatus.TRACKING_SESSION_COMPLETE_SUCCESS, response);
    }

    @PostMapping("/{sessionId}/abandon")
    @Override
    public ResponseEntity<ApiResponse<TrackingSessionResponse>> abandonSession(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId
    ) {
        TrackingSessionResponse response = trackingSessionService.abandon(userId, sessionId);
        return ApiResponse.success(SuccessStatus.TRACKING_SESSION_ABANDON_SUCCESS, response);
    }
}
