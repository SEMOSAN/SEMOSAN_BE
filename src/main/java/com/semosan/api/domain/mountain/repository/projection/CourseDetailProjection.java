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
    /** 누적 상승 고도(m). altitudes 없거나 점 1개 이하면 null. */
    Double getAscent();
    /** 누적 하강 고도(m). altitudes 없거나 점 1개 이하면 null. */
    Double getDescent();
    /** 코스 최고 고도(m). null 가능. */
    Double getMaxAltitude();
    /** GeoJSON LineString 문자열 (ST_AsGeoJSON 결과). null 가능. */
    String getPolyline();
    /** 점별 고도 배열 jsonb 문자열. null 가능. */
    String getAltitudes();
}
