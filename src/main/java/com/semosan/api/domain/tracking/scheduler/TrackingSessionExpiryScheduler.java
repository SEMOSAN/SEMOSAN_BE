package com.semosan.api.domain.tracking.scheduler;

import com.semosan.api.domain.tracking.entity.TrackingSession;
import com.semosan.api.domain.tracking.enums.TrackingSessionStatus;
import com.semosan.api.domain.tracking.event.TrackingSessionTerminatedEvent;
import com.semosan.api.domain.tracking.repository.TrackingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 24h 동안 어떤 업데이트도 없는 활성(IN_PROGRESS/PAUSED) 세션을 자동으로 ABANDONED 처리한다.
 * - 사용자가 앱을 닫고 잊거나, 네트워크 단절로 종료 신호가 안 들어온 케이스 정리.
 * - 매 1시간마다 실행.
 *
 * TODO: 만료 정책(24h) 변경 시 environment property 화. 기록 정리 시 알림 발송 여부 검토.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrackingSessionExpiryScheduler {

    private static final Duration EXPIRY_THRESHOLD = Duration.ofHours(24);

    private final TrackingSessionRepository trackingSessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 * * * *")  // 매 정시 0분 (1시간 간격)
    @Transactional
    public void expireStaleSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minus(EXPIRY_THRESHOLD);
        List<TrackingSession> stale = trackingSessionRepository.findStaleActiveSessions(
                TrackingSessionStatus.ACTIVE_STATES, cutoff);
        if (stale.isEmpty()) {
            return;
        }
        for (TrackingSession session : stale) {
            session.abandon();
            eventPublisher.publishEvent(new TrackingSessionTerminatedEvent(session.getId()));
        }
        log.info("Expired {} stale tracking sessions (cutoff={})", stale.size(), cutoff);
    }
}
