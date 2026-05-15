package com.semosan.api.domain.community.post.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.community.post.controller.docs.RecordPostControllerDocs;
import com.semosan.api.domain.community.post.dto.RecordPostCreateRequest;
import com.semosan.api.domain.community.post.dto.RecordPostResponse;
import com.semosan.api.domain.community.post.entity.RecordPost;
import com.semosan.api.domain.community.post.service.RecordPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/community/record-posts")
@RequiredArgsConstructor
public class RecordPostController implements RecordPostControllerDocs {

    private final RecordPostService recordPostService;

    @PostMapping
    public ResponseEntity<ApiResponse<RecordPostResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RecordPostCreateRequest request
    ) {
        RecordPost post = recordPostService.create(userId, request.hikingRecordId(), request.content());
        return ApiResponse.success(SuccessStatus.RECORD_POST_CREATE_SUCCESS, RecordPostResponse.from(post));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RecordPostResponse>>> getList(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<RecordPostResponse> page = recordPostService.getList(pageable).map(RecordPostResponse::from);
        return ApiResponse.success(SuccessStatus.RECORD_POST_LIST_SUCCESS, PageResponse.from(page));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<RecordPostResponse>>> getMyList(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<RecordPostResponse> page = recordPostService.getMyList(userId, pageable).map(RecordPostResponse::from);
        return ApiResponse.success(SuccessStatus.RECORD_POST_MY_LIST_SUCCESS, PageResponse.from(page));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<RecordPostResponse>> getDetail(
            @PathVariable Long postId
    ) {
        RecordPost post = recordPostService.getDetail(postId);
        return ApiResponse.success(SuccessStatus.RECORD_POST_DETAIL_SUCCESS, RecordPostResponse.from(post));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId
    ) {
        recordPostService.delete(postId, userId);
        return ApiResponse.success(SuccessStatus.RECORD_POST_DELETE_SUCCESS);
    }
}
