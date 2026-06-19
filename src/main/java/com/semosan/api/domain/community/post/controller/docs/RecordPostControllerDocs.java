package com.semosan.api.domain.community.post.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.domain.community.post.dto.RecordPostCreateRequest;
import com.semosan.api.domain.community.post.dto.RecordPostResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Record Post", description = "기록공유 게시글 API")
public interface RecordPostControllerDocs {

    @Operation(summary = "기록공유 작성", description = "등산 기록을 기반으로 게시글을 작성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "유저 또는 등산 기록 없음")
    })
    ResponseEntity<ApiResponse<RecordPostResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RecordPostCreateRequest request
    );

    @Operation(summary = "기록공유 목록", description = "기록공유 게시글 목록을 페이징으로 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<PageResponse<RecordPostResponse>>> getList(
            @AuthenticationPrincipal Long userId,
            Pageable pageable
    );

    @Operation(summary = "내 기록공유 목록", description = "내가 쓴 기록공유 게시글 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<PageResponse<RecordPostResponse>>> getMyList(
            @AuthenticationPrincipal Long userId,
            Pageable pageable
    );

    @Operation(summary = "기록공유 상세", description = "게시글 상세를 조회합니다. 호출 시 조회수가 1 증가합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "차단한 사용자의 게시글"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음 또는 삭제됨")
    })
    ResponseEntity<ApiResponse<RecordPostResponse>> getDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId
    );

    @Operation(summary = "기록공유 삭제", description = "본인의 게시글을 soft delete 합니다.")
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
