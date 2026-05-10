package com.semosan.api.domain.community.post.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.community.post.dto.FreePostCreateRequest;
import com.semosan.api.domain.community.post.dto.FreePostDetailResponse;
import com.semosan.api.domain.community.post.dto.FreePostListResponse;
import com.semosan.api.domain.community.post.service.FreePostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/community/free-posts")
@RequiredArgsConstructor
public class FreePostController {

    private final FreePostService freePostService;

    @PostMapping
    public ResponseEntity<ApiResponse<FreePostDetailResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FreePostCreateRequest request
    ) {
        FreePostDetailResponse response = freePostService.create(
                userId,
                request.title(),
                request.content(),
                request.imageUrls(),
                request.mainImageIndex()
        );
        return ApiResponse.success(SuccessStatus.FREE_POST_CREATE_SUCCESS, response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FreePostListResponse>>> getList(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(
                SuccessStatus.FREE_POST_LIST_SUCCESS,
                PageResponse.from(freePostService.getList(pageable))
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<FreePostListResponse>>> getMyList(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(
                SuccessStatus.FREE_POST_MY_LIST_SUCCESS,
                PageResponse.from(freePostService.getMyList(userId, pageable))
        );
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<FreePostDetailResponse>> getDetail(
            @PathVariable Long postId
    ) {
        return ApiResponse.success(
                SuccessStatus.FREE_POST_DETAIL_SUCCESS,
                freePostService.getDetail(postId)
        );
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId
    ) {
        freePostService.delete(postId, userId);
        return ApiResponse.success(SuccessStatus.FREE_POST_DELETE_SUCCESS);
    }
}
