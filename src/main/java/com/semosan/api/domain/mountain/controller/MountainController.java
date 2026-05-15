package com.semosan.api.domain.mountain.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.mountain.controller.docs.MountainControllerDocs;
import com.semosan.api.domain.mountain.dto.response.LikedMountainResponse;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse;
import com.semosan.api.domain.mountain.dto.response.MountainListResponse;
import com.semosan.api.domain.mountain.dto.response.MountainMapListResponse;
import com.semosan.api.domain.mountain.dto.response.MountainRecommendationResponse;
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

    /**
     * 지도에 표시할 산 목록을 BBox(Bounding Box, 사각형 영역) 기준으로 조회한다.
     * BBox는 사각형의 두 꼭짓점 좌표로 정의된다.
     *  - sw* (South-West): 남서쪽 꼭짓점 (사각형의 좌측 하단)
     *  - ne* (North-East): 북동쪽 꼭짓점 (사각형의 우측 상단)
     * 네 값이 모두 null이면 서비스 레이어에서 서울 기본 BBox가 적용된다.
     */
    @GetMapping("/map")
    @Override
    public ResponseEntity<ApiResponse<MountainMapListResponse>> getMountainsForMap(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Double swLat,
            @RequestParam(required = false) Double swLng,
            @RequestParam(required = false) Double neLat,
            @RequestParam(required = false) Double neLng
    ) {
        MountainMapListResponse response = mountainService.getMountainsForMap(userId, swLat, swLng, neLat, neLng);
        return ApiResponse.success(SuccessStatus.MOUNTAIN_MAP_SUCCESS, response);
    }

    @GetMapping("/recommendations")
    @Override
    public ResponseEntity<ApiResponse<PageResponse<MountainRecommendationResponse>>> getRecommendedMountains(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        PageResponse<MountainRecommendationResponse> response = PageResponse.from(
                mountainService.getRecommendedMountains(userId, pageable)
        );
        return ApiResponse.success(SuccessStatus.MOUNTAIN_RECOMMENDATION_SUCCESS, response);
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
