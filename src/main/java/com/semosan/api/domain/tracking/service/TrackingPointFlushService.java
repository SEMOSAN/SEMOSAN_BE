package com.semosan.api.domain.tracking.service;

import com.semosan.api.domain.tracking.entity.TrackingPoint;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import com.semosan.api.domain.tracking.repository.TrackingPointRepository;
import com.semosan.api.domain.tracking.repository.TrackingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    private final TrackingPointRepository trackingPointRepository;
    private final TrackingSessionRepository trackingSessionRepository;

    /**
     * 주어진 점들을 단일 트랜잭션 안에서 저장한다.
     * 세션이 이미 사라진 경우 0 을 반환 (호출자가 점을 폐기하도록).
     * 반환값 = 저장된 점 수.
     */
    @Transactional
    public int flush(Long sessionId, List<PendingPoint> pendings) {
        if (pendings == null || pendings.isEmpty()) {
            return 0;
        }
        Optional<TrackingSession> sessionOpt = trackingSessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            log.warn("Tracking session {} not found while flushing; discarding {} points",
                    sessionId, pendings.size());
            return 0;
        }
        TrackingSession session = sessionOpt.get();

        List<TrackingPoint> batch = pendings.stream()
                .map(p -> {
                    Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(p.lng(), p.lat()));
                    location.setSRID(4326);
                    return TrackingPoint.create(session, location, p.altitude(), p.recordedAt());
                })
                .toList();
        trackingPointRepository.saveAll(batch);
        return batch.size();
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
