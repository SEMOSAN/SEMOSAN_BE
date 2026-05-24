package com.semosan.api.domain.mountain.repository.projection;

/**
 * 코스 상세 native query 결과.
 * polyline / altitudes 는 PostGIS 와 jsonb 를 그대로 JSON 문자열로 받아
 * 응답 DTO 에서 @JsonRawValue 로 통과시킨다.
 */
public interface CourseDetailProjection {
    Long getId();
    String getName();
    String getDifficulty();
    Double getDistance();
    Integer getDuration();
    String getStartName();
    String getEndName();
    /** GeoJSON LineString 문자열 (ST_AsGeoJSON 결과). null 가능. */
    String getPolyline();
    /** 점별 고도 배열 jsonb 문자열. null 가능. */
    String getAltitudes();
}
