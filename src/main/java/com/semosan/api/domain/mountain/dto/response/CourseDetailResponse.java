package com.semosan.api.domain.mountain.dto.response;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.semosan.api.domain.mountain.enums.Difficulty;
import com.semosan.api.domain.mountain.repository.projection.CourseDetailProjection;

/**
 * 코스 상세 응답 — 지도 polyline + 고도 단면 포함.
 *  - polyline: GeoJSON LineString. ST_AsGeoJSON 결과를 그대로 JSON 으로 통과시킴.
 *  - altitudes: 점별 고도 배열(jsonb). polyline.coordinates[i] 와 1:1 대응.
 *               nullable — 고도 데이터 없는 코스는 null.
 */
public record CourseDetailResponse(
        Long id,
        String name,
        Difficulty difficulty,
        Double distance,
        Integer duration,
        String startName,
        String endName,
        @JsonRawValue String polyline,
        @JsonRawValue String altitudes
) {
    public static CourseDetailResponse from(CourseDetailProjection p) {
        return new CourseDetailResponse(
                p.getId(),
                p.getName(),
                p.getDifficulty() == null ? null : Difficulty.valueOf(p.getDifficulty()),
                p.getDistance(),
                p.getDuration(),
                p.getStartName(),
                p.getEndName(),
                p.getPolyline(),
                p.getAltitudes()
        );
    }
}
