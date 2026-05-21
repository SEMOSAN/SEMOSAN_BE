package com.semosan.api.domain.tracking.scheduler;

import com.semosan.api.domain.tracking.entity.TrackingSession;
import com.semosan.api.domain.tracking.enums.TrackingSessionStatus;
import com.semosan.api.domain.tracking.event.TrackingSessionTerminatedEvent;
import com.semosan.api.domain.tracking.repository.TrackingSessionRepository;
import com.semosan.api.domain.tracking.service.TrackingSessionActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    private final TrackingSessionActivityService activityService;

    @Scheduled(cron = "0 0 * * * *")  // 매 정시 0분 (1시간 간격)
    @Transactional
    public void expireStaleSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minus(EXPIRY_THRESHOLD);
        List<TrackingSession> candidates = trackingSessionRepository.findStaleActiveSessions(
                TrackingSessionStatus.ACTIVE_STATES, cutoff);
        if (candidates.isEmpty()) {
            return;
        }
        int expired = 0;
        for (TrackingSession session : candidates) {
            // DB updatedAt 은 GPS 수신 시 갱신되지 않으므로 Redis 의 마지막 활동 시각으로 재검증.
            Optional<LocalDateTime> lastActive = activityService.getLastActive(session.getId());
            if (lastActive.isPresent() && lastActive.get().isAfter(cutoff)) {
                continue;
            }
            session.abandon();
            eventPublisher.publishEvent(new TrackingSessionTerminatedEvent(session.getId()));
            expired++;
        }
        log.info("Expired {}/{} stale tracking sessions (cutoff={})",
                expired, candidates.size(), cutoff);
    }
}
