package com.semosan.api.domain.tracking.event;

/**
 * 트래킹 세션이 종료(COMPLETED 또는 ABANDONED)됐을 때 발행.
 * 소비자(예: GPS 스트림 consumer)는 sessionId 에 묶인 메모리 버퍼를 정리할 책임을 진다.
 * AFTER_COMMIT phase 로만 받도록 — 종료 트랜잭션이 롤백되면 버퍼는 그대로 유지돼야 한다.
 */
public record TrackingSessionTerminatedEvent(Long sessionId) {
}
