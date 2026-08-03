package com.semosan.api.domain.admin.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.domain.admin.dto.request.AdminMountainUpdateRequest;
import com.semosan.api.domain.admin.dto.request.AdminMountainVisibilityRequest;
import com.semosan.api.domain.admin.dto.request.AdminRestaurantRequest;
import com.semosan.api.domain.admin.dto.request.AdminRestaurantSectionRequest;
import com.semosan.api.domain.admin.dto.request.AdminTransportationRequest;
import com.semosan.api.domain.admin.dto.response.AdminMountainListResponse;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Admin Mountain", description = "관리자 산 정보 관리 API")
public interface AdminMountainControllerDocs {

    @Operation(summary = "산 목록 조회", description = "공개/비공개 포함 전체 산 목록을 조회합니다. keyword 파라미터로 이름 또는 주소 검색이 가능합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "산 목록 조회 성공")
    })
    ResponseEntity<ApiResponse<PageResponse<AdminMountainListResponse>>> getMountains(
            @Parameter(description = "검색 키워드 (이름 또는 주소)") @RequestParam(required = false) String keyword,
            @Parameter(description = "공개 상태 필터 (ALL, PUBLIC, PRIVATE)") @RequestParam(required = false) String visibility,
            Pageable pageable
    );

    @Operation(summary = "산 상세 조회", description = "공개/비공개 관계없이 산의 상세 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "산 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "산을 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<MountainDetailResponse>> getMountainDetail(@PathVariable Long mountainId);

    @Operation(summary = "산 정보 수정", description = "산의 이름, 주소, 고도, 난이도, 소요시간, 이미지를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "산 정보 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "산을 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<Void>> updateMountain(
            @PathVariable Long mountainId,
            @Valid @RequestBody AdminMountainUpdateRequest request
    );

    @Operation(summary = "산 공개/비공개 처리", description = "산의 공개 상태를 변경합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공개 상태 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "산을 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<Void>> updateVisibility(
            @PathVariable Long mountainId,
            @Valid @RequestBody AdminMountainVisibilityRequest request
    );

    @Operation(summary = "맛집 섹션 생성", description = "산에 맛집 섹션을 추가합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "맛집 섹션 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "산을 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<Long>> createRestaurantSection(
            @PathVariable Long mountainId,
            @Valid @RequestBody AdminRestaurantSectionRequest request
    );

    @Operation(summary = "맛집 섹션 수정", description = "맛집 섹션의 제목을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "맛집 섹션 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "맛집 섹션을 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<Void>> updateRestaurantSection(
            @PathVariable Long sectionId,
            @Valid @RequestBody AdminRestaurantSectionRequest request
    );

    @Operation(summary = "맛집 섹션 삭제", description = "맛집 섹션과 소속 맛집을 모두 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "맛집 섹션 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "맛집 섹션을 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<Void>> deleteRestaurantSection(@PathVariable Long sectionId);

    @Operation(summary = "맛집 추가", description = "맛집 섹션에 맛집을 추가합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "맛집 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "맛집 섹션을 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<Long>> createRestaurant(
            @PathVariable Long sectionId,
            @Valid @RequestBody AdminRestaurantRequest request
    );

    @Operation(summary = "맛집 수정", description = "맛집 정보를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "맛집 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "맛집을 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<Void>> updateRestaurant(
            @PathVariable Long restaurantId,
            @Valid @RequestBody AdminRestaurantRequest request
    );

    @Operation(summary = "맛집 삭제", description = "맛집을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "맛집 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "맛집을 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<Void>> deleteRestaurant(@PathVariable Long restaurantId);

    @Operation(summary = "교통정보 추가", description = "산에 교통정보를 추가합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "교통정보 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "산을 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<Long>> createTransportation(
            @PathVariable Long mountainId,
            @Valid @RequestBody AdminTransportationRequest request
    );

    @Operation(summary = "교통정보 수정", description = "교통정보를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "교통정보 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "교통정보를 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<Void>> updateTransportation(
            @PathVariable Long transportationId,
            @Valid @RequestBody AdminTransportationRequest request
    );

    @Operation(summary = "교통정보 삭제", description = "교통정보를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "교통정보 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "교통정보를 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<Void>> deleteTransportation(@PathVariable Long transportationId);
}
