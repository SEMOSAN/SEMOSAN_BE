package com.semosan.api.domain.tracking.scheduler;

import com.semosan.api.domain.tracking.service.TrackingPointCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 버려진(ABANDONED) 세션이 남긴 고아 GPS 좌표를 주기적으로 정리한다.
 *
 * 배경:
 *  - ABANDONED 세션은 HikingRecord 로 변환되지 않으므로 그 좌표를 읽는 경로가 없다.
 *    FK 에 ON DELETE CASCADE 도 없어 지금까지 tracking_points 에 영구히 쌓여 왔다.
 *  - 세션 하나가 좌표 1,000 건대를 남긴다.
 *
 * 종료 직후가 아니라 유예 기간을 두고 지우는 이유:
 *  1) 24h 무업데이트로 자동 만료된 세션(사용자가 폐기를 선택한 게 아님)에 대해
 *     문의가 들어올 여지를 남긴다.
 *  2) 세션 종료 직후에는 아직 좌표가 들어올 수 있다.
 *     - TrackingStreamConsumer 가 AFTER_COMMIT 으로 잔여 버퍼를 final flush 한다.
 *     - Redis Stream 에 이미 쌓여 있던 메시지는 세션 상태와 무관하게 소비된다.
 *     둘 다 수십 초 범위라 하루 유예면 경합이 발생하지 않는다.
 *
 * 세션 행 자체는 남긴다 — 유니크 인덱스가 부분 인덱스(IN_PROGRESS/PAUSED)라
 * 새 세션 생성을 막지 않고, 포기율 지표로 쓸 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrackingPointCleanupScheduler {

    /** 세션 종료 후 좌표를 남겨두는 기간. */
    private static final Duration RETENTION = Duration.ofDays(1);
    /** 한 트랜잭션에서 처리할 세션 수. */
    private static final int BATCH_SIZE = 100;
    /**
     * 1회 실행의 배치 상한. 첫 실행처럼 누적분이 많아도 한 번에 다 끌어안지 않게 막는다.
     * 남은 대상은 다음 실행에서 이어서 처리된다.
     */
    private static final int MAX_BATCHES = 50;

    private final TrackingPointCleanupService cleanupService;

    @Scheduled(cron = "0 0 4 * * *")  // 매일 04:00 — 트래픽이 가장 적은 시간대
    public void cleanupOrphanedPoints() {
        LocalDateTime cutoff = LocalDateTime.now().minus(RETENTION);
        int totalSessions = 0;
        int totalPoints = 0;
        int batches = 0;

        while (batches < MAX_BATCHES) {
            TrackingPointCleanupService.BatchResult result;
            try {
                result = cleanupService.cleanupBatch(cutoff, BATCH_SIZE);
            } catch (RuntimeException e) {
                // 한 배치가 실패해도 이미 지운 배치는 커밋된 상태다. 다음 실행에서 이어서 처리된다.
                log.error("Orphaned tracking point cleanup failed after {} batches (cutoff={})",
                        batches, cutoff, e);
                return;
            }
            if (result.isEmpty()) {
                break;
            }
            totalSessions += result.sessionCount();
            totalPoints += result.deletedPoints();
            batches++;
        }

        if (batches >= MAX_BATCHES) {
            log.warn("[CLEANUP] 배치 상한({}) 도달 — 남은 대상은 다음 실행에서 처리됩니다", MAX_BATCHES);
        }
        if (totalSessions > 0) {
            log.info("[CLEANUP] 완료 | 세션 {}개 | 좌표 {}건 | cutoff={}", totalSessions, totalPoints, cutoff);
        }
    }
}
