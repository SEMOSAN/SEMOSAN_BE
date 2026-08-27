package com.semosan.api.domain.tracking.service;

import com.semosan.api.domain.tracking.repository.TrackingPointJdbcRepository;
import com.semosan.api.domain.tracking.repository.TrackingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 누적된 GPS 점 배치를 tracking_points 테이블에 저장한다.
 *
 * 별도 서비스로 분리한 이유:
 *  - {@link TrackingStreamConsumer} 내부에서 @Transactional 메서드를 self-invocation 으로 호출하면
 *    Spring AOP proxy 가 적용되지 않아 트랜잭션이 무효화된다.
 *  - 외부 빈으로 분리하면 호출 시 proxy 를 거치므로 @Transactional 이 정상 동작한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingPointFlushService {

    /** 클라 시계 오차 허용 — 이 폭 이상으로 미래/과거이면 점 폐기. */
    private static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(5);
    private static final Duration PAST_TOLERANCE = Duration.ofHours(24);

    private final TrackingPointJdbcRepository trackingPointJdbcRepository;
    private final TrackingSessionRepository trackingSessionRepository;

    /**
     * 주어진 점들을 단일 트랜잭션 안에서 JDBC batch insert 로 저장한다.
     * 세션이 이미 사라진 경우 0 을 반환 (호출자가 점을 폐기하도록).
     * 반환값 = 저장된 점 수.
     */
    @Transactional
    public int flush(Long sessionId, List<PendingPoint> pendings) {
        if (pendings == null || pendings.isEmpty()) {
            return 0;
        }
        if (!trackingSessionRepository.existsById(sessionId)) {
            log.warn("Tracking session {} not found while flushing; discarding {} points",
                    sessionId, pendings.size());
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        List<PendingPoint> validPoints = pendings.stream()
                .filter(p -> isValidRecordedAt(sessionId, p.recordedAt(), now))
                .toList();
        if (validPoints.isEmpty()) {
            return 0;
        }

        return trackingPointJdbcRepository.saveAllInBatch(sessionId, validPoints, now);
    }

    /** recordedAt 이 null 이거나 허용 범위(과거 24h ~ 미래 5분)를 벗어나면 false. */
    private boolean isValidRecordedAt(Long sessionId, LocalDateTime recordedAt, LocalDateTime now) {
        if (recordedAt == null) {
            log.warn("Discarding tracking point: recordedAt is null (session={})", sessionId);
            return false;
        }
        if (recordedAt.isAfter(now.plus(FUTURE_TOLERANCE))) {
            log.warn("Discarding tracking point: recordedAt too far in future (session={}, recordedAt={})",
                    sessionId, recordedAt);
            return false;
        }
        if (recordedAt.isBefore(now.minus(PAST_TOLERANCE))) {
            log.warn("Discarding tracking point: recordedAt too old (session={}, recordedAt={})",
                    sessionId, recordedAt);
            return false;
        }
        return true;
    }

    /** Consumer 메모리 버퍼에 누적되는 점 단위 — 외부에서 참조 가능하도록 노출. */
    public record PendingPoint(
            double lat,
            double lng,
            Double altitude,
            LocalDateTime recordedAt
    ) {
    }
}
