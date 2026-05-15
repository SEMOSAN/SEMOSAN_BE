package com.semosan.api.domain.community.post.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.domain.community.post.dto.FreePostCreateRequest;
import com.semosan.api.domain.community.post.dto.FreePostDetailResponse;
import com.semosan.api.domain.community.post.dto.FreePostListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Free Post", description = "자유게시판 API")
public interface FreePostControllerDocs {

    @Operation(summary = "자유게시판 작성", description = "제목/본문/이미지(여러 장 + 대표 인덱스)로 게시글을 작성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "본문 누락 또는 대표 이미지 인덱스 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "유저 없음")
    })
    ResponseEntity<ApiResponse<FreePostDetailResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FreePostCreateRequest request
    );

    @Operation(summary = "자유게시판 목록", description = "최신순 페이징. 응답에 대표이미지/좋아요수/댓글수가 함께 내려갑니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<PageResponse<FreePostListResponse>>> getList(Pageable pageable);

    @Operation(summary = "내 자유게시판 목록", description = "내가 쓴 자유게시판 게시글 목록.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<PageResponse<FreePostListResponse>>> getMyList(
            @AuthenticationPrincipal Long userId,
            Pageable pageable
    );

    @Operation(summary = "자유게시판 상세", description = "게시글 상세 (전체 이미지 + 카운트). 호출 시 조회수 +1.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음 또는 삭제됨")
    })
    ResponseEntity<ApiResponse<FreePostDetailResponse>> getDetail(@PathVariable Long postId);

    @Operation(summary = "자유게시판 삭제", description = "본인의 게시글을 soft delete 합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 게시글 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId
    );
}
