package com.semosan.api.domain.community.like.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.community.like.dto.PostLikeToggleResponse;
import com.semosan.api.domain.community.like.service.PostLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/community/posts/{postId}/likes")
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeService postLikeService;

    @PostMapping
    public ResponseEntity<ApiResponse<PostLikeToggleResponse>> toggle(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId
    ) {

        return ApiResponse.success(
                SuccessStatus.POST_LIKE_TOGGLE_SUCCESS,
                postLikeService.toggleWithCount(postId, userId)
        );
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getCount(
            @PathVariable Long postId
    ) {
        return ApiResponse.success(SuccessStatus.POST_LIKE_COUNT_SUCCESS, postLikeService.count(postId));
    }
}
