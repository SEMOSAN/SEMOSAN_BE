package com.semosan.api.domain.tracking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;

/**
 * 진행 중/종료된 트래킹 세션의 이동 경로 응답.
 * 앱을 껐다 켠 뒤 지도에 지금까지의 경로를 다시 그리는 용도.
 *
 *  - track / altitudes: PostGIS / jsonb 결과를 그대로 통과시킨다 (raw JSON 직렬화).
 *    HikingRecordDetailResponse 와 동일한 방식.
 *  - 점이 0~1개면 ST_MakeLine 이 null 을 반환하므로 track / altitudes 모두 null 로 나간다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TrackingTrackResponse(
        Long sessionId,
        @JsonRawValue String track,
        @JsonRawValue String altitudes
) {

    public static TrackingTrackResponse of(Long sessionId, String track, String altitudes) {
        return new TrackingTrackResponse(sessionId, track, altitudes);
    }

    /** 아직 저장된 점이 없거나 1개뿐인 세션. */
    public static TrackingTrackResponse empty(Long sessionId) {
        return new TrackingTrackResponse(sessionId, null, null);
    }
}
