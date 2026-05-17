package com.semosan.api.domain.mountain.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.domain.mountain.dto.response.LikedMountainResponse;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse;
import com.semosan.api.domain.mountain.dto.response.MountainListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Mountain", description = "산 관련 API")
public interface MountainControllerDocs {

    @Operation(
            summary = "산 목록 조회",
            description = "등록된 모든 산의 목록을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "산 목록 조회 성공"
            )
    })
    ResponseEntity<ApiResponse<PageResponse<MountainListResponse>>> getMountains(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10) Pageable pageable
    );

    @Operation(
            summary = "산 검색",
            description = "산 이름 또는 주소로 검색합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "산 검색 성공"
            )
    })
    ResponseEntity<ApiResponse<PageResponse<MountainListResponse>>> searchMountains(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "검색 키워드 (산 이름 또는 주소)", required = true)
            @RequestParam String keyword,
            @PageableDefault(size = 10) Pageable pageable
    );

    @Operation(
            summary = "좋아요한 산 목록 조회",
            description = "로그인한 사용자가 좋아요한 산 목록을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "좋아요한 산 목록 조회 성공"
            )
    })
    ResponseEntity<ApiResponse<PageResponse<LikedMountainResponse>>> getLikedMountains(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10) Pageable pageable
    );

    @Operation(
            summary = "산 상세 정보 조회",
            description = "산의 상세 정보를 조회합니다. 코스, 교통, 편의시설, 맛집, 리뷰 정보를 포함합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "산 상세 정보 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "산을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<MountainDetailResponse>> getMountainDetail(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "산 ID", required = true)
            @PathVariable Long mountainId
    );

    @Operation(
            summary = "산 좋아요 등록",
            description = "로그인한 사용자가 산에 좋아요를 등록합니다. 이미 좋아요한 산이면 성공 처리합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "산 좋아요 등록 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "산을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<Void>> likeMountain(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "산 ID", required = true)
            @PathVariable Long mountainId
    );

    @Operation(
            summary = "산 좋아요 취소",
            description = "로그인한 사용자가 산 좋아요를 취소합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "산 좋아요 취소 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "좋아요한 산이 아니거나 산을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<Void>> unlikeMountain(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "산 ID", required = true)
            @PathVariable Long mountainId
    );
}
