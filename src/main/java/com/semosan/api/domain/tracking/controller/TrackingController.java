package com.semosan.api.domain.tracking.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.tracking.controller.docs.TrackingControllerDocs;
import com.semosan.api.domain.tracking.dto.response.NearbyMountainResponse;
import com.semosan.api.domain.tracking.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController implements TrackingControllerDocs {

    private final TrackingService trackingService;

    /**
     * 트래킹 진입 화면에서 호출.
     * 프론트가 디바이스 GPS 좌표(lat, lng)를 보내면, 가장 가까운 산과 그 산의 코스 목록을 반환한다.
     * 인증 필수 — 트래킹은 로그인 사용자만 시작 가능.
     */
    @GetMapping("/nearby-mountain")
    @Override
    public ResponseEntity<ApiResponse<NearbyMountainResponse>> getNearbyMountain(
            @AuthenticationPrincipal Long userId,
            @RequestParam Double lat,
            @RequestParam Double lng
    ) {
        NearbyMountainResponse response = trackingService.getNearbyMountain(userId, lat, lng);
        return ApiResponse.success(SuccessStatus.TRACKING_NEAREST_MOUNTAIN_SUCCESS, response);
    }
}
