package com.semosan.api.domain.mountain.enums;

/**
 * 코스 polyline 의 segment 별 경사 등급.
 *  - 임계: ±5%, ±15% (gradePercent = Δaltitude / Δhorizontalmeters × 100)
 *  - 클라이언트는 등급별 색상으로 polyline sub-구간을 그린다.
 */
public enum SlopeGrade {

    STEEP_DOWN,
    MILD_DOWN,
    FLAT,
    MILD_UP,
    STEEP_UP;

    private static final double MILD_THRESHOLD = 5.0;
    private static final double STEEP_THRESHOLD = 15.0;

    public static SlopeGrade classify(double gradePercent) {
        if (gradePercent <= -STEEP_THRESHOLD) {
            return STEEP_DOWN;
        }
        if (gradePercent <= -MILD_THRESHOLD) {
            return MILD_DOWN;
        }
        if (gradePercent < MILD_THRESHOLD) {
            return FLAT;
        }
        if (gradePercent < STEEP_THRESHOLD) {
            return MILD_UP;
        }
        return STEEP_UP;
    }
}
