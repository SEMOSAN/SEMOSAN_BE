package com.semosan.api.domain.admin.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.domain.admin.dto.request.AdminSemoFeedVisibilityRequest;
import com.semosan.api.domain.admin.dto.response.AdminSemoFeedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Admin SemoFeed", description = "관리자 세모피드 관리 API")
public interface AdminSemoFeedControllerDocs {

    @Operation(summary = "세모피드 목록 조회", description = "공개/비공개 포함 전체 세모피드 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "세모피드 목록 조회 성공")
    })
    ResponseEntity<ApiResponse<PageResponse<AdminSemoFeedResponse>>> getFeeds(Pageable pageable);

    @Operation(summary = "세모피드 공개/비공개 처리", description = "세모피드의 공개 상태를 변경합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공개 상태 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세모피드를 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<Void>> updateVisibility(
            @PathVariable Long semoFeedId,
            @Valid @RequestBody AdminSemoFeedVisibilityRequest request
    );

    @Operation(summary = "세모피드 강제 삭제", description = "세모피드를 강제로 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "세모피드 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세모피드를 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long semoFeedId);
}
