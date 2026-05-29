package com.semosan.api.domain.tracking.repository.projection;

/**
 * 트래킹 세션 한 건의 이동 경로 native query 결과.
 *  - track: PostGIS LineString 을 GeoJSON 으로 변환한 문자열. 점이 0~1개면 null.
 *  - altitudes: 점별 고도(jsonb 배열) 를 그대로 문자열로. 점이 0개면 "[null]" 등이 될 수 있어 호출자에서 처리한다.
 * 응답 DTO 에서 @JsonRawValue 로 통과시킨다.
 */
public interface TrackingPathProjection {
    String getTrack();
    String getAltitudes();
}
