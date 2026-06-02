package com.semosan.api.domain.mountain.dto.response;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.semosan.api.domain.mountain.enums.Difficulty;
import com.semosan.api.domain.mountain.repository.projection.CourseDetailProjection;

import java.util.List;

/**
 * 코스 상세 응답 — 지도 polyline + 고도 단면 + 경사 등급 segments 포함.
 *  - polyline: GeoJSON LineString. ST_AsGeoJSON 결과를 그대로 JSON 으로 통과시킴.
 *  - altitudes: 점별 고도 배열(jsonb). polyline.coordinates[i] 와 1:1 대응.
 *               nullable — 고도 데이터 없는 코스는 null.
 *  - segments: polyline 의 연속된 같은 경사 등급 구간. 점이 2개 미만이거나 altitudes 가 비면 빈 배열.
 */
public record CourseDetailResponse(
        Long id,
        Long mountainId,
        String name,
        Difficulty difficulty,
        Double distance,
        Integer duration,
        String startName,
        String endName,
        Double ascent,
        Double descent,
        Double maxAltitude,
        boolean likedByMe,
        @JsonRawValue String polyline,
        @JsonRawValue String altitudes,
        List<SlopeSegmentResponse> segments
) {
    public static CourseDetailResponse from(
            CourseDetailProjection p,
            boolean likedByMe,
            List<SlopeSegmentResponse> segments
    ) {
        return new CourseDetailResponse(
                p.getId(),
                p.getMountainId(),
                p.getName(),
                p.getDifficulty() == null ? null : Difficulty.valueOf(p.getDifficulty()),
                p.getDistance(),
                p.getDuration(),
                p.getStartName(),
                p.getEndName(),
                p.getAscent(),
                p.getDescent(),
                p.getMaxAltitude(),
                likedByMe,
                p.getPolyline(),
                p.getAltitudes(),
                segments
        );
    }
}
