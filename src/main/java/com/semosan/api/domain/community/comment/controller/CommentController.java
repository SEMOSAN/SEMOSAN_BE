package com.semosan.api.domain.community.comment.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.community.comment.controller.docs.CommentControllerDocs;
import com.semosan.api.domain.community.comment.dto.CommentCreateRequest;
import com.semosan.api.domain.community.comment.dto.CommentReplyRequest;
import com.semosan.api.domain.community.comment.dto.CommentResponse;
import com.semosan.api.domain.community.comment.service.CommentService;
import com.semosan.api.domain.user.service.UserBlockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController implements CommentControllerDocs {

    private final CommentService commentService;
    private final UserBlockService userBlockService;

    @PostMapping("/community/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> create(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        return ApiResponse.success(SuccessStatus.COMMENT_CREATE_SUCCESS,
                CommentResponse.from(commentService.create(postId, userId, request.content())));
    }

    @PostMapping("/community/posts/{postId}/comments/replies")
    public ResponseEntity<ApiResponse<CommentResponse>> reply(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody CommentReplyRequest request
    ) {
        return ApiResponse.success(SuccessStatus.COMMENT_REPLY_SUCCESS,
                CommentResponse.from(commentService.reply(
                        postId, userId, request.parentId(), request.mentionedUserId(), request.content()
                )));
    }

    @GetMapping("/community/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getComments(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ApiResponse.success(
                SuccessStatus.COMMENT_LIST_SUCCESS,
                PageResponse.from(commentService.getCommentsByPost(postId, userId, pageable))
        );
    }

    @GetMapping("/community/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getReplies(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long commentId
    ) {
        return ApiResponse.success(
                SuccessStatus.COMMENT_REPLY_LIST_SUCCESS,
                commentService.getReplies(commentId, userId)
        );
    }

    @DeleteMapping("/community/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long commentId
    ) {
        commentService.delete(commentId, userId);
        return ApiResponse.success(SuccessStatus.COMMENT_DELETE_SUCCESS);
    }

    @PostMapping("/community/comments/{commentId}/blocks")
    public ResponseEntity<ApiResponse<Void>> block(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long commentId
    ) {
        userBlockService.blockByComment(userId, commentId);
        return ApiResponse.success(SuccessStatus.COMMENT_BLOCK_SUCCESS);
    }
}
