package com.semosan.api.domain.tracking.service;

import com.semosan.api.common.config.TrackingProperties;
import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.tracking.dto.message.GpsPointMessage;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import com.semosan.api.domain.tracking.enums.TrackingSessionStatus;
import com.semosan.api.domain.tracking.repository.TrackingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StringRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 트래킹 GPS 점을 Redis Stream 으로 발행하기 전, 세션 소유자/상태를 검증한다.
 *  - 세션 존재 + 본인 소유 검증은 강제 (위반 시 throw → WebSocket 연결 강제 종료 효과)
 *  - PAUSED/COMPLETED/ABANDONED 상태에서는 silent drop (예외 X) — 클라이언트 race 흡수
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingGpsPublisher {

    private static final String FIELD_SESSION_ID = "sessionId";
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_LAT = "lat";
    private static final String FIELD_LNG = "lng";
    private static final String FIELD_ALTITUDE = "altitude";
    private static final String FIELD_RECORDED_AT = "recordedAt";

    private final StringRedisTemplate redisTemplate;
    private final TrackingProperties trackingProperties;
    private final TrackingSessionRepository trackingSessionRepository;

    @Transactional(readOnly = true)
    public void publish(Long userId, Long sessionId, GpsPointMessage message) {
        TrackingSession session = trackingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.TRACKING_SESSION_NOT_FOUND));
        if (!session.isOwnedBy(userId)) {
            throw new GeneralException(ErrorStatus.TRACKING_SESSION_FORBIDDEN);
        }
        if (session.getStatus() != TrackingSessionStatus.IN_PROGRESS) {
            log.debug("Dropping GPS point: sessionId={} status={}", sessionId, session.getStatus());
            return;
        }

        Map<String, String> body = Map.of(
                FIELD_SESSION_ID, String.valueOf(sessionId),
                FIELD_USER_ID, String.valueOf(userId),
                FIELD_LAT, String.valueOf(message.lat()),
                FIELD_LNG, String.valueOf(message.lng()),
                FIELD_ALTITUDE, message.altitude() == null ? "" : String.valueOf(message.altitude()),
                FIELD_RECORDED_AT, message.recordedAt().toString()
        );
        StringRecord record = StringRecord.of(body).withStreamKey(trackingProperties.getStreamKey());
        RecordId id = redisTemplate.opsForStream().add(record);
        log.trace("Published GPS point: sessionId={} streamId={}", sessionId, id);
    }
}
