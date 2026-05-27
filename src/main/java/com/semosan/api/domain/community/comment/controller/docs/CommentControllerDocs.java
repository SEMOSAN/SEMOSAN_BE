package com.semosan.api.domain.community.comment.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.domain.community.comment.dto.CommentCreateRequest;
import com.semosan.api.domain.community.comment.dto.CommentReplyRequest;
import com.semosan.api.domain.community.comment.dto.CommentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Comment", description = "댓글/대댓글 API (자유게시판/기록공유 공용)")
public interface CommentControllerDocs {

    @Operation(summary = "댓글 작성", description = "게시글에 1뎁스 댓글을 작성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 또는 유저 없음")
    })
    ResponseEntity<ApiResponse<CommentResponse>> create(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request
    );

    @Operation(
            summary = "대댓글 작성",
            description = "특정 댓글에 답글을 답니다. 부모가 대댓글이면 1뎁스 댓글로 자동 정규화 (트리 깊이 2 유지)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "부모 댓글이 다른 게시글의 댓글"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글/댓글/유저 없음")
    })
    ResponseEntity<ApiResponse<CommentResponse>> reply(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody CommentReplyRequest request
    );

    @Operation(summary = "댓글 목록", description = "특정 게시글의 1뎁스 댓글 목록 (페이징).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getComments(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            Pageable pageable
    );

    @Operation(summary = "대댓글 목록", description = "특정 1뎁스 댓글의 대댓글 목록 (시간순). 차단한 사용자의 댓글은 내용이 '차단한 사용자입니다.'로 대체됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<List<CommentResponse>>> getReplies(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long commentId
    );

    @Operation(summary = "댓글 삭제", description = "본인의 댓글/대댓글을 soft delete 합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 댓글 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long commentId
    );
}
