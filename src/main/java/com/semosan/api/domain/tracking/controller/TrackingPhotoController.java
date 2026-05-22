package com.semosan.api.domain.tracking.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.tracking.controller.docs.TrackingPhotoControllerDocs;
import com.semosan.api.domain.tracking.dto.request.TrackingPhotoUploadRequest;
import com.semosan.api.domain.tracking.dto.response.TrackingPhotoResponse;
import com.semosan.api.domain.tracking.service.TrackingPhotoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tracking/sessions/{sessionId}/photos")
@RequiredArgsConstructor
public class TrackingPhotoController implements TrackingPhotoControllerDocs {

    private final TrackingPhotoService trackingPhotoService;

    @PostMapping
    @Override
    public ResponseEntity<ApiResponse<TrackingPhotoResponse>> upload(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId,
            @Valid @RequestBody TrackingPhotoUploadRequest request
    ) {
        TrackingPhotoResponse response = trackingPhotoService.upload(userId, sessionId, request);
        return ApiResponse.success(SuccessStatus.TRACKING_PHOTO_UPLOAD_SUCCESS, response);
    }

    @GetMapping
    @Override
    public ResponseEntity<ApiResponse<List<TrackingPhotoResponse>>> list(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId
    ) {
        List<TrackingPhotoResponse> response = trackingPhotoService.listBySession(userId, sessionId);
        return ApiResponse.success(SuccessStatus.TRACKING_PHOTO_LIST_SUCCESS, response);
    }
}
