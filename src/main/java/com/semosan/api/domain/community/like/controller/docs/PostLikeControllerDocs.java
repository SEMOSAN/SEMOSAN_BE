package com.semosan.api.domain.community.like.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.domain.community.like.dto.PostLikeToggleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Post Like", description = "게시글 좋아요 API (자유게시판/기록공유 공용)")
public interface PostLikeControllerDocs {

    @Operation(
            summary = "좋아요 토글",
            description = "이미 눌렀으면 취소, 안 눌렀으면 좋아요. 응답으로 결과 상태와 총 카운트 반환."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토글 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 또는 유저 없음")
    })
    ResponseEntity<ApiResponse<PostLikeToggleResponse>> toggle(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId
    );

    @Operation(summary = "좋아요 수 조회", description = "특정 게시글의 좋아요 수.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    ResponseEntity<ApiResponse<Long>> getCount(@PathVariable Long postId);
}
