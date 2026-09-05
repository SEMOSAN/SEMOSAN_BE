package com.semosan.api.domain.semofeed.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.domain.semofeed.dto.SemoFeedCreateRequest;
import com.semosan.api.domain.semofeed.dto.SemoFeedEmojiRequest;
import com.semosan.api.domain.semofeed.dto.SemoFeedEmojiToggleResponse;
import com.semosan.api.domain.semofeed.dto.SemoFeedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "SemoFeed", description = "세모피드 API")
public interface SemoFeedControllerDocs {

    @Operation(
            summary = "세모피드 저장",
            description = "트래킹 사진으로 생성된 세모피드 이미지를 저장합니다. 저장 시 비공개 상태로 생성됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "세모피드 저장 성공",
                    content = @Content(schema = @Schema(implementation = SemoFeedResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<SemoFeedResponse>> create(
            @AuthenticationPrincipal Long userId,
            @RequestBody SemoFeedCreateRequest request
    );

    @Operation(
            summary = "공개 세모피드 목록 조회",
            description = "공개 상태인 세모피드를 최신순으로 최대 100개 페이징 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "공개 세모피드 목록 조회 성공"
            )
    })
    ResponseEntity<ApiResponse<PageResponse<SemoFeedResponse>>> listPublic(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 100) Pageable pageable
    );

    @Operation(
            summary = "세모피드 단건 조회",
            description = "세모피드 하나를 조회합니다. 알림에서 해당 게시물로 이동할 때 사용합니다. "
                    + "비공개 세모피드는 작성자 본인만 조회할 수 있습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "세모피드 조회 성공",
                    content = @Content(schema = @Schema(implementation = SemoFeedResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "비공개 세모피드는 작성자만 조회 가능 (SF_403_1)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세모피드를 찾을 수 없음 (SF_404_1)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<SemoFeedResponse>> get(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "세모피드 ID", required = true)
            @PathVariable Long semoFeedId
    );

    @Operation(
            summary = "세모피드 이모지 토글",
            description = "세모피드에 FIRE, HEART, CONGRATS, LAUGH 이모지를 타입별로 등록하거나 취소합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "세모피드 이모지 처리 성공",
                    content = @Content(schema = @Schema(implementation = SemoFeedEmojiToggleResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세모피드를 찾을 수 없음 (SF_404_1)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<SemoFeedEmojiToggleResponse>> toggleEmoji(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "세모피드 ID", required = true)
            @PathVariable Long semoFeedId,
            @Valid @RequestBody SemoFeedEmojiRequest request
    );

    @Operation(
            summary = "내 세모피드 목록 조회",
            description = "로그인한 사용자의 세모피드를 최신순으로 전체 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "내 세모피드 목록 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<List<SemoFeedResponse>>> listMine(
            @AuthenticationPrincipal Long userId
    );

    @Operation(
            summary = "세모피드 공개/비공개 토글",
            description = "본인의 세모피드 공개 상태를 토글합니다. 공개 시 모든 사용자의 피드에 노출됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "공개 상태 변경 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "본인의 세모피드만 처리 가능 (SF_403_1)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세모피드를 찾을 수 없음 (SF_404_1)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<Boolean>> togglePublic(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "세모피드 ID", required = true)
            @PathVariable Long semoFeedId
    );

    @Operation(
            summary = "세모피드 삭제",
            description = "본인의 세모피드를 삭제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "세모피드 삭제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "본인의 세모피드만 처리 가능 (SF_403_1)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세모피드를 찾을 수 없음 (SF_404_1)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "세모피드 ID", required = true)
            @PathVariable Long semoFeedId
    );
}
