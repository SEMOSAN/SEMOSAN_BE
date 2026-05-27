package com.semosan.api.domain.community.post.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.community.post.controller.docs.FreePostControllerDocs;
import com.semosan.api.domain.community.post.dto.FreePostCreateRequest;
import com.semosan.api.domain.community.post.dto.FreePostDetailResponse;
import com.semosan.api.domain.community.post.dto.FreePostListResponse;
import com.semosan.api.domain.community.post.dto.FreePostReportRequest;
import com.semosan.api.domain.community.post.service.FreePostReportService;
import com.semosan.api.domain.community.post.service.FreePostService;
import com.semosan.api.domain.user.service.UserBlockService;
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
public class FreePostController implements FreePostControllerDocs {

    private final FreePostService freePostService;
    private final FreePostReportService freePostReportService;
    private final UserBlockService userBlockService;

    @PostMapping
    public ResponseEntity<ApiResponse<FreePostDetailResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FreePostCreateRequest request
    ) {

        return ApiResponse.success(SuccessStatus.FREE_POST_CREATE_SUCCESS, freePostService.create(
                userId,
                request.title(),
                request.content(),
                request.imageUrls(),
                request.mainImageIndex()
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FreePostListResponse>>> getList(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(
                SuccessStatus.FREE_POST_LIST_SUCCESS,
                PageResponse.from(freePostService.getList(userId, pageable))
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<FreePostListResponse>>> search(
            @AuthenticationPrincipal Long userId,
            @RequestParam String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(
                SuccessStatus.FREE_POST_SEARCH_SUCCESS,
                PageResponse.from(freePostService.search(userId, keyword, pageable))
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
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId
    ) {
        return ApiResponse.success(
                SuccessStatus.FREE_POST_DETAIL_SUCCESS,
                freePostService.getDetail(userId, postId)
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

    @PostMapping("/{postId}/reports")
    public ResponseEntity<ApiResponse<Void>> report(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody FreePostReportRequest request
    ) {
        freePostReportService.report(userId, postId, request.reason());
        return ApiResponse.success(SuccessStatus.FREE_POST_REPORT_SUCCESS);
    }

    @PostMapping("/{postId}/blocks")
    public ResponseEntity<ApiResponse<Void>> block(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId
    ) {
        userBlockService.blockByPost(userId, postId);
        return ApiResponse.success(SuccessStatus.FREE_POST_BLOCK_SUCCESS);
    }
}
