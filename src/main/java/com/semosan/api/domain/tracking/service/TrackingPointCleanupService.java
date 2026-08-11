package com.semosan.api.domain.tracking.service;

import com.semosan.api.domain.tracking.enums.TrackingSessionStatus;
import com.semosan.api.domain.tracking.repository.TrackingPointRepository;
import com.semosan.api.domain.tracking.repository.TrackingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 버려진(ABANDONED) 세션이 남긴 고아 GPS 좌표를 배치 단위로 삭제한다.
 *
 * 별도 서비스로 분리한 이유:
 *  - 스케줄러가 배치를 반복 호출하는데, 같은 클래스 안에서 @Transactional 메서드를
 *    self-invocation 하면 Spring AOP proxy 를 거치지 않아 트랜잭션이 무효화된다.
 *    {@link TrackingPointFlushService} 와 동일한 이유의 분리.
 *  - 배치마다 트랜잭션을 끊어야 첫 실행처럼 대상이 많을 때 긴 트랜잭션이
 *    tracking_points 를 오래 잡고 있는 상황을 피할 수 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingPointCleanupService {

    private final TrackingSessionRepository trackingSessionRepository;
    private final TrackingPointRepository trackingPointRepository;

    /**
     * 정리 대상 세션을 batchSize 만큼 찾아 좌표를 벌크 삭제한다.
     *
     * @return 이번 배치에서 처리한 세션 수. 0 이면 더 이상 대상이 없다는 뜻이라 호출자가 루프를 끝내면 된다.
     */
    @Transactional
    public BatchResult cleanupBatch(LocalDateTime cutoff, int batchSize) {
        List<Long> sessionIds = trackingSessionRepository.findSessionIdsForPointCleanup(
                TrackingSessionStatus.ABANDONED,
                cutoff,
                PageRequest.of(0, batchSize)
        );
        if (sessionIds.isEmpty()) {
            return new BatchResult(0, 0);
        }
        int deletedPoints = trackingPointRepository.deleteByTrackingSessionIdIn(sessionIds);
        log.info("[CLEANUP] 고아 좌표 삭제 | 세션 {}개 | 좌표 {}건", sessionIds.size(), deletedPoints);
        return new BatchResult(sessionIds.size(), deletedPoints);
    }

    public record BatchResult(int sessionCount, int deletedPoints) {
        public boolean isEmpty() {
            return sessionCount == 0;
        }
    }
}
