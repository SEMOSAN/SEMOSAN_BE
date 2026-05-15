package com.semosan.api.domain.mountain.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.mountain.controller.docs.MountainControllerDocs;
import com.semosan.api.domain.mountain.dto.response.LikedMountainResponse;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse;
import com.semosan.api.domain.mountain.dto.response.MountainListResponse;
import com.semosan.api.domain.mountain.service.MountainLikeService;
import com.semosan.api.domain.mountain.service.MountainService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mountains")
@RequiredArgsConstructor
public class MountainController implements MountainControllerDocs {

    private final MountainService mountainService;
    private final MountainLikeService mountainLikeService;

    @GetMapping
    @Override
    public ResponseEntity<ApiResponse<PageResponse<MountainListResponse>>> getMountains(
            @PageableDefault(size = 10, sort = "name") Pageable pageable
    ) {
        PageResponse<MountainListResponse> response = PageResponse.from(mountainService.getMountains(pageable));
        return ApiResponse.success(SuccessStatus.MOUNTAIN_LIST_SUCCESS, response);
    }

    @GetMapping("/search")
    @Override
    public ResponseEntity<ApiResponse<PageResponse<MountainListResponse>>> searchMountains(
            @RequestParam String keyword,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        PageResponse<MountainListResponse> response = PageResponse.from(mountainService.searchMountains(keyword, pageable));
        return ApiResponse.success(SuccessStatus.MOUNTAIN_SEARCH_SUCCESS, response);
    }

    @GetMapping("/likes")
    @Override
    public ResponseEntity<ApiResponse<PageResponse<LikedMountainResponse>>> getLikedMountains(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        PageResponse<LikedMountainResponse> response = PageResponse.from(mountainLikeService.getLikedMountains(userId, pageable));
        return ApiResponse.success(SuccessStatus.LIKED_MOUNTAIN_LIST_SUCCESS, response);
    }

    @GetMapping("/{mountainId}")
    @Override
    public ResponseEntity<ApiResponse<MountainDetailResponse>> getMountainDetail(
            @PathVariable Long mountainId
    ) {
        MountainDetailResponse response = mountainService.getMountainDetail(mountainId);
        return ApiResponse.success(SuccessStatus.MOUNTAIN_DETAIL_SUCCESS, response);
    }

    @PostMapping("/{mountainId}/like")
    @Override
    public ResponseEntity<ApiResponse<Void>> likeMountain(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long mountainId
    ) {
        mountainLikeService.likeMountain(userId, mountainId);
        return ApiResponse.success(SuccessStatus.MOUNTAIN_LIKE_SUCCESS);
    }

    @DeleteMapping("/{mountainId}/like")
    @Override
    public ResponseEntity<ApiResponse<Void>> unlikeMountain(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long mountainId
    ) {
        mountainLikeService.unlikeMountain(userId, mountainId);
        return ApiResponse.success(SuccessStatus.MOUNTAIN_UNLIKE_SUCCESS);
    }
}
