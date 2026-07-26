package com.semosan.api.domain.admin.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.domain.admin.dto.request.AdminUserSuspendRequest;
import com.semosan.api.domain.admin.dto.response.AdminReportedPostResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Admin Community", description = "관리자 커뮤니티 관리 API")
public interface AdminCommunityControllerDocs {

    @Operation(summary = "신고된 게시글 목록 조회", description = "신고 횟수가 많은 순으로 게시글 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<Page<AdminReportedPostResponse>>> getReportedPosts(Pageable pageable);

    @Operation(summary = "게시글 강제 삭제", description = "게시글을 강제로 삭제(숨김) 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long postId);

    @Operation(summary = "댓글 강제 삭제", description = "댓글을 강제로 삭제 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long commentId);

    @Operation(summary = "사용자 정지", description = "사용자를 지정된 기간까지 정지합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정지 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<Void>> suspendUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserSuspendRequest request
    );

    @Operation(summary = "사용자 정지 해제", description = "사용자의 정지를 해제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정지 해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<Void>> unsuspendUser(@PathVariable Long userId);
}
