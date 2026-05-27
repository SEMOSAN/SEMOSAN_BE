package com.semosan.api.domain.hiking.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.hiking.controller.docs.HikingRecordControllerDocs;
import com.semosan.api.domain.hiking.dto.request.CreateCourseDifficultyFeedbackRequest;
import com.semosan.api.domain.hiking.dto.response.CourseDifficultyFeedbackResponse;
import com.semosan.api.domain.hiking.dto.response.GetUserHikingRecordResponse;
import com.semosan.api.domain.hiking.dto.response.GetUserHikingMountainRecordResponse;
import com.semosan.api.domain.hiking.dto.response.GetUserHikingRecordSummaryResponse;
import com.semosan.api.domain.hiking.service.HikingRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hiking-records")
@RequiredArgsConstructor
public class HikingRecordController implements HikingRecordControllerDocs {

    private final HikingRecordService hikingRecordService;

    // 나의 등산 기록 목록을 조회합니다.
    @GetMapping("/me")
    @Override
    public ResponseEntity<ApiResponse<PageResponse<GetUserHikingRecordResponse>>> getUserHikingRecords(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        PageResponse<GetUserHikingRecordResponse> response = PageResponse.from(hikingRecordService.getUserHikingRecords(userId, pageable));
        return ApiResponse.success(SuccessStatus.GET_HIKING_RECORD_LIST_SUCCESS, response);
    }

    // 내가 다녀온 산 목록을 조회합니다.
    @GetMapping("/me/mountains")
    @Override
    public ResponseEntity<ApiResponse<PageResponse<GetUserHikingMountainRecordResponse>>> getUserHikingMountainRecords(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        PageResponse<GetUserHikingMountainRecordResponse> response = PageResponse.from(hikingRecordService.getUserHikingMountainRecords(userId, pageable));
        return ApiResponse.success(SuccessStatus.GET_HIKING_RECORD_MOUNTAIN_LIST_SUCCESS, response);
    }

    // 특정 산에 대한 나의 등산 기록 목록(코스 단위)을 조회합니다.
    @GetMapping("/me/mountains/{mountainId}")
    @Override
    public ResponseEntity<ApiResponse<PageResponse<GetUserHikingRecordResponse>>> getUserHikingRecordsByMountainId(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long mountainId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        PageResponse<GetUserHikingRecordResponse> response = PageResponse.from(
                hikingRecordService.getUserHikingRecordsByMountainId(userId, mountainId, pageable)
        );
        return ApiResponse.success(SuccessStatus.GET_HIKING_RECORD_LIST_BY_MOUNTAIN_SUCCESS, response);
    }

    // 나의 등산 기록 요약 정보를 조회합니다.
    @GetMapping("/me/summary")
    @Override
    public ResponseEntity<ApiResponse<GetUserHikingRecordSummaryResponse>> getUserHikingRecordSummary(
            @AuthenticationPrincipal Long userId
    ) {
        GetUserHikingRecordSummaryResponse response = hikingRecordService.getUserHikingRecordSummary(userId);
        return ApiResponse.success(SuccessStatus.GET_HIKING_RECORD_SUMMARY_SUCCESS, response);
    }

    // 코스 난이도 피드백을 저장합니다.
    @PostMapping("/{hikingRecordId}/difficulty-feedback")
    @Override
    public ResponseEntity<ApiResponse<CourseDifficultyFeedbackResponse>> createCourseDifficultyFeedback(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long hikingRecordId,
            @Valid @RequestBody CreateCourseDifficultyFeedbackRequest request
    ) {
        CourseDifficultyFeedbackResponse response = hikingRecordService.createCourseDifficultyFeedback(
                userId,
                hikingRecordId,
                request
        );
        return ApiResponse.success(SuccessStatus.CREATE_COURSE_DIFFICULTY_FEEDBACK_SUCCESS, response);
    }
}
