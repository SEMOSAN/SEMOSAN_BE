package com.semosan.api.domain.hiking.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.hiking.controller.docs.HikingRecordControllerDocs;
import com.semosan.api.domain.hiking.dto.response.GetUserHikingRecordResponse;
import com.semosan.api.domain.hiking.service.HikingRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hiking-records")
@RequiredArgsConstructor
public class HikingRecordController implements HikingRecordControllerDocs {

    private final HikingRecordService hikingRecordService;

    // 내가 다녀온 산 목록을 조회합니다.
    @GetMapping("/me/mountains")
    @Override
    public ResponseEntity<ApiResponse<PageResponse<GetUserHikingRecordResponse>>> getUserHikingRecords(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        PageResponse<GetUserHikingRecordResponse> response = PageResponse.from(hikingRecordService.getUserHikingRecords(userId, pageable));
        return ApiResponse.success(SuccessStatus.GET_HIKING_RECORD_LIST_SUCCESS, response);
    }
}
