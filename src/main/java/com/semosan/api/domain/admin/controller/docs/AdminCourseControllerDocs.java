package com.semosan.api.domain.admin.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.domain.admin.dto.request.AdminCourseCreateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Admin Course", description = "관리자 코스 API")
public interface AdminCourseControllerDocs {

    @Operation(
            summary = "코스 생성",
            description = "지도에서 찍은 좌표 목록으로 코스를 만듭니다.\n\n"
                    + "- `distance` 는 요청에 담지 않습니다. 서버가 좌표로 직접 계산합니다 "
                    + "(트래킹 누적 거리와 같은 공식이어야 마일스톤 푸시 지점이 맞습니다).\n"
                    + "- 고도는 저장하지 않습니다. 지도 클릭으로는 위경도만 얻기 때문입니다. "
                    + "그래서 이 코스의 경사도 세그먼트는 빈 배열로 나갑니다.\n"
                    + "- `summitPointIndex` 로 좌표 하나를 정상으로 지정할 수 있습니다. "
                    + "지정하지 않으면 사진 마일스톤이 정상 기준이 아니라 거리 4등분으로 fallback 됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "코스 생성 성공 (data 는 생성된 courseId)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "정상 지점 인덱스가 좌표 범위를 벗어남"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "산을 찾을 수 없음"
            )
    })
    ResponseEntity<ApiResponse<Long>> create(@Valid @RequestBody AdminCourseCreateRequest request);

    @Operation(
            summary = "코스 삭제",
            description = "코스를 삭제합니다. 등산 기록·리뷰·좋아요·트래킹 세션 등이 연결된 코스는 "
                    + "사용자 데이터를 함께 지우게 되므로 삭제하지 않고 409 로 거부합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "코스 삭제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "코스를 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "연결된 데이터가 있어 삭제 불가"
            )
    })
    ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long courseId);
}
