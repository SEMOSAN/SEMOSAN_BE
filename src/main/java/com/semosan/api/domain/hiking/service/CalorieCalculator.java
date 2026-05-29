package com.semosan.api.domain.hiking.service;

/**
 * 등산 칼로리 추정.
 *
 * 공식: kcal = MET × 체중(kg) × 시간(시간)
 *  - 체중이 등록돼 있지 않으면 한국 성인 평균치({@value #DEFAULT_WEIGHT_KG}kg) 로 대체한다.
 *  - 등산 강도(MET) 는 누적 상승고도(ascent) / 거리(distance) 비율로 단순 분기.
 *    Compendium of Physical Activities 2011 의 hiking 항목을 참고했다.
 *
 * 이 클래스는 정적 호출 전용이라 인스턴스화하지 않는다.
 */
public final class CalorieCalculator {

    // TODO: 체중 미등록 시 한국 성인 평균치(65kg) 로 일괄 대체 중. 향후 사용자 성별·나이 기반 추정값
    //       또는 온보딩 단계에서 체중 입력 강제로 정확도 개선 검토.
    static final double DEFAULT_WEIGHT_KG = 65.0;

    private static final double MET_EASY = 6.0;
    private static final double MET_MEDIUM = 7.5;
    private static final double MET_HARD = 9.0;

    private static final double MEDIUM_GRADE_RATIO = 0.05;
    private static final double HARD_GRADE_RATIO = 0.10;

    private CalorieCalculator() {
    }

    public static int calculate(
            Double weightKg,
            Double distanceMeters,
            Double ascentMeters,
            int durationSeconds
    ) {
        if (durationSeconds <= 0) {
            return 0;
        }
        double effectiveWeight = (weightKg != null && weightKg > 0) ? weightKg : DEFAULT_WEIGHT_KG;
        double met = decideMet(distanceMeters, ascentMeters);
        double hours = durationSeconds / 3600.0;
        return (int) Math.round(met * effectiveWeight * hours);
    }

    private static double decideMet(Double distanceMeters, Double ascentMeters) {
        if (distanceMeters == null || distanceMeters <= 0 || ascentMeters == null || ascentMeters <= 0) {
            return MET_EASY;
        }
        double ratio = ascentMeters / distanceMeters;
        if (ratio < MEDIUM_GRADE_RATIO) {
            return MET_EASY;
        }
        if (ratio < HARD_GRADE_RATIO) {
            return MET_MEDIUM;
        }
        return MET_HARD;
    }
}
