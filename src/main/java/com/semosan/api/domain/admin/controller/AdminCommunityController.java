package com.semosan.api.domain.admin.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.admin.controller.docs.AdminCommunityControllerDocs;
import com.semosan.api.domain.admin.dto.request.AdminUserSuspendRequest;
import com.semosan.api.domain.admin.dto.response.AdminReportedPostResponse;
import com.semosan.api.domain.admin.service.AdminCommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminCommunityController implements AdminCommunityControllerDocs {

    private final AdminCommunityService adminCommunityService;

    @GetMapping("/community/reported-posts")
    @Override
    public ResponseEntity<ApiResponse<Page<AdminReportedPostResponse>>> getReportedPosts(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<AdminReportedPostResponse> result = adminCommunityService.getReportedPosts(pageable);
        return ApiResponse.success(SuccessStatus.ADMIN_REPORTED_POST_LIST_SUCCESS, result);
    }

    @DeleteMapping("/community/posts/{postId}")
    @Override
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long postId) {
        adminCommunityService.deletePost(postId);
        return ApiResponse.success(SuccessStatus.ADMIN_POST_DELETE_SUCCESS);
    }

    @DeleteMapping("/community/comments/{commentId}")
    @Override
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long commentId) {
        adminCommunityService.deleteComment(commentId);
        return ApiResponse.success(SuccessStatus.ADMIN_COMMENT_DELETE_SUCCESS);
    }

    @PostMapping("/users/{userId}/suspend")
    @Override
    public ResponseEntity<ApiResponse<Void>> suspendUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserSuspendRequest request
    ) {
        adminCommunityService.suspendUser(userId, request);
        return ApiResponse.success(SuccessStatus.ADMIN_USER_SUSPEND_SUCCESS);
    }

    @DeleteMapping("/users/{userId}/suspend")
    @Override
    public ResponseEntity<ApiResponse<Void>> unsuspendUser(@PathVariable Long userId) {
        adminCommunityService.unsuspendUser(userId);
        return ApiResponse.success(SuccessStatus.ADMIN_USER_UNSUSPEND_SUCCESS);
    }
}
