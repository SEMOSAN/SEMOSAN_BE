package com.semosan.api.domain.tracking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 트래킹 세션 정상 종료 요청.
 *
 * body 전체가 선택이다. 비워 보내면 자유기록은 서버가 기본 이름을 만들어 채우고,
 * 코스 기록은 코스명으로 표시된다.
 */
public record CompleteTrackingSessionRequest(

        @Schema(
                description = "기록 이름. 자유기록에서 비워두면 서버가 `260723_등산왕의코스1` 형태로 채우고, "
                        + "코스 기록에서 비워두면 코스명으로 표시된다.",
                example = "북한산 아침 산책"
        )
        @Size(max = 30, message = "기록 이름은 30자 이하여야 합니다.")
        String name
) {
}
