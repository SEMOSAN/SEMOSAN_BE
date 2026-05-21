package com.semosan.api.domain.tracking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Redis Stream(tracking:gps) 의 GPS 점 메시지를 소비한다.
 *  - 메시지 수신 즉시: 실시간 통계(Redis Hash) 갱신 → #46 라이브 액티비티/푸시에서 활용
 *  - 메모리 버퍼에 적재 → 10초 또는 100개 단위로 별도 서비스에 넘겨 DB 배치 insert
 *
 * 트랜잭션 경계:
 *  - DB 적재는 {@link TrackingPointFlushService#flush} 를 외부 빈 호출로 위임한다.
 *    같은 클래스 self-invocation 으로는 Spring AOP proxy 가 적용되지 않아 @Transactional 이 무효화된다.
 *
 * 주의:
 *  - 현재 단일 인스턴스 가정. 다중 인스턴스 시 consumer 이름 분리(host/pod name) + 동시성 안전 통계 처리 필요.
 *  - 버퍼는 in-memory 라 프로세스 다운 시 마지막 N초치 손실 가능. 운영 임계 시 트랜잭셔널 outbox 패턴 검토.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrackingStreamConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private static final int FLUSH_THRESHOLD = 100;

    private static final String F_SESSION_ID = "sessionId";
    private static final String F_LAT = "lat";
    private static final String F_LNG = "lng";
    private static final String F_ALTITUDE = "altitude";
    private static final String F_RECORDED_AT = "recordedAt";

    private final Map<Long, Queue<TrackingPointFlushService.PendingPoint>> buffers = new ConcurrentHashMap<>();

    private final TrackingSessionStatsService statsService;
    private final TrackingPointFlushService flushService;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        Map<String, String> body = message.getValue();
        try {
            Long sessionId = Long.parseLong(body.get(F_SESSION_ID));
            double lat = Double.parseDouble(body.get(F_LAT));
            double lng = Double.parseDouble(body.get(F_LNG));
            Double altitude = parseNullableDouble(body.get(F_ALTITUDE));
            LocalDateTime recordedAt = LocalDateTime.parse(body.get(F_RECORDED_AT));

            statsService.recordPoint(sessionId, lat, lng, altitude, recordedAt);

            Queue<TrackingPointFlushService.PendingPoint> queue =
                    buffers.computeIfAbsent(sessionId, k -> new ConcurrentLinkedQueue<>());
            queue.offer(new TrackingPointFlushService.PendingPoint(lat, lng, altitude, recordedAt));

            if (queue.size() >= FLUSH_THRESHOLD) {
                flushSession(sessionId);
            }
        } catch (RuntimeException e) {
            log.warn("Failed to process GPS stream message: id={} body={}", message.getId(), body, e);
        }
    }

    /** 매 10초마다 모든 세션 버퍼를 DB 로 flush. */
    @Scheduled(fixedDelay = 10_000L)
    public void flushAll() {
        for (Long sessionId : buffers.keySet()) {
            flushSession(sessionId);
        }
    }

    private void flushSession(Long sessionId) {
        Queue<TrackingPointFlushService.PendingPoint> queue = buffers.get(sessionId);
        if (queue == null || queue.isEmpty()) {
            return;
        }
        List<TrackingPointFlushService.PendingPoint> batch = new ArrayList<>();
        TrackingPointFlushService.PendingPoint p;
        while ((p = queue.poll()) != null) {
            batch.add(p);
            if (batch.size() >= FLUSH_THRESHOLD) {
                break;
            }
        }
        if (batch.isEmpty()) {
            return;
        }
        try {
            int saved = flushService.flush(sessionId, batch);
            if (saved > 0) {
                log.debug("Flushed {} GPS points for session {}", saved, sessionId);
            }
        } catch (RuntimeException e) {
            // DB flush 실패 시 batch 를 큐로 되돌려 다음 주기에 재시도.
            // ConcurrentLinkedQueue 는 tail-only offer 라 순서가 뒤섞일 수 있으나 recordedAt 정렬로 보정 가능.
            log.error("Failed to flush {} GPS points for session {}; re-queued for retry",
                    batch.size(), sessionId, e);
            batch.forEach(queue::offer);
        }
    }

    private static Double parseNullableDouble(String value) {
        return (value == null || value.isEmpty()) ? null : Double.parseDouble(value);
    }
}
