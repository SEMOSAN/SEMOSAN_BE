package com.semosan.api.domain.tracking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 트래킹 세션 정상 종료 요청.
 *
 * body 전체가 선택이다. 코스 기록은 코스명으로 표시되므로 보낼 값이 없고,
 * 자유기록도 이름을 비워 보내면 서버가 기본 이름을 만들어 채운다.
 */
public record CompleteTrackingSessionRequest(

        @Schema(
                description = "자유기록 이름. 비워두면 서버가 `260723_등산왕의코스1` 형태로 채운다. "
                        + "코스 기록에서는 값을 보내도 무시된다.",
                example = "북한산 아침 산책"
        )
        @Size(max = 30, message = "기록 이름은 30자 이하여야 합니다.")
        String name
) {
}
