package com.semosan.api.domain.admin.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.admin.controller.docs.AdminSemoFeedControllerDocs;
import com.semosan.api.domain.admin.dto.request.AdminSemoFeedVisibilityRequest;
import com.semosan.api.domain.admin.dto.response.AdminSemoFeedResponse;
import com.semosan.api.domain.admin.service.AdminSemoFeedService;
import com.semosan.api.domain.semofeed.dto.SemoFeedCreateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminSemoFeedController implements AdminSemoFeedControllerDocs {

    private final AdminSemoFeedService adminSemoFeedService;

    @PostMapping("/semofeed")
    @Override
    public ResponseEntity<ApiResponse<AdminSemoFeedResponse>> create(
            @Valid @RequestBody SemoFeedCreateRequest request
    ) {
        AdminSemoFeedResponse response = adminSemoFeedService.create(request.imageUrl());
        return ApiResponse.success(SuccessStatus.ADMIN_SEMOFEED_CREATE_SUCCESS, response);
    }

    @GetMapping("/semofeed")
    @Override
    public ResponseEntity<ApiResponse<PageResponse<AdminSemoFeedResponse>>> getFeeds(
            @PageableDefault(size = 24) Pageable pageable
    ) {
        PageResponse<AdminSemoFeedResponse> result = PageResponse.from(
                adminSemoFeedService.getFeeds(pageable)
        );
        return ApiResponse.success(SuccessStatus.ADMIN_SEMOFEED_LIST_SUCCESS, result);
    }

    @PatchMapping("/semofeed/{semoFeedId}/visibility")
    @Override
    public ResponseEntity<ApiResponse<Void>> updateVisibility(
            @PathVariable Long semoFeedId,
            @Valid @RequestBody AdminSemoFeedVisibilityRequest request
    ) {
        adminSemoFeedService.updateVisibility(semoFeedId, request.isPublic());
        return ApiResponse.success(SuccessStatus.ADMIN_SEMOFEED_VISIBILITY_UPDATE_SUCCESS);
    }

    @DeleteMapping("/semofeed/{semoFeedId}")
    @Override
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long semoFeedId) {
        adminSemoFeedService.delete(semoFeedId);
        return ApiResponse.success(SuccessStatus.ADMIN_SEMOFEED_DELETE_SUCCESS);
    }
}
