package com.semosan.api.domain.tracking.enums;

public enum TrackingSessionStatus {
    /** 트래킹 진행 중 */
    IN_PROGRESS,
    /** 사용자 일시정지 — 점 수집 멈춤, duration 에서 제외 */
    PAUSED,
    /** 정상 종료 — HikingRecord 변환은 #20 에서 수행 */
    COMPLETED,
    /** 사용자 포기 또는 24h 무업데이트로 자동 만료 */
    ABANDONED
}
