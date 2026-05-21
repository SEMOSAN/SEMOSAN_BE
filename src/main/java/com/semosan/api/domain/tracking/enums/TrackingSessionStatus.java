package com.semosan.api.domain.tracking.enums;

import java.util.EnumSet;
import java.util.Set;

public enum TrackingSessionStatus {
    /** 트래킹 진행 중 */
    IN_PROGRESS,
    /** 사용자 일시정지 — 점 수집 멈춤, duration 에서 제외 */
    PAUSED,
    /** 정상 종료 — HikingRecord 변환은 #20 에서 수행 */
    COMPLETED,
    /** 사용자 포기 또는 24h 무업데이트로 자동 만료 */
    ABANDONED;

    /** 활성(트래킹 가능) 상태 집합. 진행/일시정지 모두 포함. */
    public static final Set<TrackingSessionStatus> ACTIVE_STATES = EnumSet.of(IN_PROGRESS, PAUSED);
}
