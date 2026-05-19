package com.semosan.api.domain.mountain.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.domain.mountain.dto.response.LikedMountainResponse;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse;
import com.semosan.api.domain.mountain.dto.response.MountainListResponse;
import com.semosan.api.domain.mountain.dto.response.MountainMapListResponse;
import com.semosan.api.domain.mountain.dto.response.MountainRecommendationResponse;
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
            @Parameter(description = "검색 키워드 (산 이름 또는 주소)", required = true)
            @RequestParam String keyword,
            @PageableDefault(size = 10) Pageable pageable
    );

    @Operation(
            summary = "지도 영역 내 산 조회 (홈 화면)",
            description = "지도 화면에 표시할 산 목록을 BBox(남서/북동 좌표) 기준으로 조회합니다. "
                    + "로그인한 사용자의 등산 기록을 기반으로 visited, visitCount, imageUrl이 채워집니다. "
                    + "BBox 4개 좌표가 모두 비어있으면 서울 기본 영역이 적용됩니다. "
                    + "1~3개만 채워진 부분 입력은 의도가 모호하므로 400 으로 거부합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "지도 영역 내 산 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "BBox 4개 좌표 중 일부만 보낸 경우 (MTN_400_1)"
            )
    })
    ResponseEntity<ApiResponse<MountainMapListResponse>> getMountainsForMap(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "BBox 남서쪽 꼭짓점의 위도 (사각형 아래쪽 변, 예: 37.413)")
            @RequestParam(required = false) Double swLat,
            @Parameter(description = "BBox 남서쪽 꼭짓점의 경도 (사각형 왼쪽 변, 예: 126.764)")
            @RequestParam(required = false) Double swLng,
            @Parameter(description = "BBox 북동쪽 꼭짓점의 위도 (사각형 위쪽 변, 예: 37.715)")
            @RequestParam(required = false) Double neLat,
            @Parameter(description = "BBox 북동쪽 꼭짓점의 경도 (사각형 오른쪽 변, 예: 127.184)")
            @RequestParam(required = false) Double neLng
    );

    @Operation(
            summary = "레벨 맞춤 산 추천 (홈 화면)",
            description = "로그인 사용자의 등산 레벨(HikingLevel) → 산 난이도(Difficulty) 매핑으로 후보를 추리고, "
                    + "사용자 위치(lat, lng)에서 가까운 순으로 정렬해 페이지 단위로 반환합니다. "
                    + "온보딩이 완료되지 않은 사용자는 모든 난이도가 fallback으로 사용됩니다. "
                    + "Pageable.sort 는 무시됩니다 (서버 정책상 '거리 가까운 순' 고정)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "레벨 맞춤 산 추천 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "lat 또는 lng 파라미터 누락",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<PageResponse<MountainRecommendationResponse>>> getRecommendedMountains(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "사용자 현재 위치 위도 (필수)", required = true, example = "37.4533700")
            @RequestParam Double lat,
            @Parameter(description = "사용자 현재 위치 경도 (필수)", required = true, example = "126.9571678")
            @RequestParam Double lng,
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
