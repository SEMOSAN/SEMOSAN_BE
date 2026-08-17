package com.semosan.api.domain.tracking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import com.semosan.api.domain.tracking.service.TrackingMilestoneTriggerService;
import com.semosan.api.domain.tracking.service.TrackingSessionStatsService;

import java.util.List;

/**
 * 앱 재실행 후 진행 중이던 트래킹을 이어서 하기 위한 복원 스냅샷.
 *
 * 이동 경로는 여기 담지 않는다 — 좌표가 수천 개까지 커질 수 있어 무게와 호출 주기가 다르므로
 * GET /api/tracking/sessions/{sessionId}/track 으로 분리했다.
 * 촬영된 사진 목록도 담지 않는다 — GET /api/tracking/sessions/{sessionId}/photos 가 이미 있다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TrackingRestoreResponse(
        TrackingSessionResponse session,
        /** 일시정지 시간을 제외한 실제 경과 시간(초). */
        long elapsedSeconds,
        /** Redis 통계 TTL(24h)이 만료됐으면 null — 클라이언트는 복원 불가로 분기한다. */
        Stats stats,
        PhotoMilestone photoMilestone
) {

    /**
     * 진행 중 누적 통계. 출처는 Redis Hash 이며 DB 좌표로 재계산하지 않는다.
     * complete 시 HikingRecord 에 남는 값과 같은 출처여야 최종 기록과 숫자가 어긋나지 않는다.
     */
    public record Stats(
            double distanceMeters,
            double ascentMeters,
            double descentMeters,
            Double maxAltitudeMeters,
            long pointCount
    ) {
        public static Stats from(TrackingSessionStatsService.Stats stats) {
            return new Stats(
                    stats.distanceMeters(),
                    stats.ascentMeters(),
                    stats.descentMeters(),
                    stats.maxAltitudeMeters(),
                    stats.pointCount()
            );
        }
    }

    /**
     * 사진 마일스톤 진행 상태.
     * openedIndexes 에서 closedIndexes 를 빼면 지금 열려 있는 촬영 창이 된다.
     */
    public record PhotoMilestone(
            List<Double> milestones,
            List<Integer> openedIndexes,
            List<Integer> closedIndexes,
            boolean summitNotified
    ) {
        public static PhotoMilestone from(TrackingMilestoneTriggerService.MilestoneState state) {
            return new PhotoMilestone(
                    state.milestones(),
                    state.openedIndexes(),
                    state.closedIndexes(),
                    state.summitNotified()
            );
        }
    }

    public static TrackingRestoreResponse of(
            TrackingSession session,
            TrackingSessionStatsService.Stats stats,
            TrackingMilestoneTriggerService.MilestoneState milestoneState
    ) {
        return new TrackingRestoreResponse(
                TrackingSessionResponse.from(session),
                session.elapsedSeconds(),
                // 점이 한 번도 안 들어왔거나 Redis 키가 만료된 경우를 같은 형태(null)로 취급한다.
                stats.pointCount() == 0 ? null : Stats.from(stats),
                PhotoMilestone.from(milestoneState)
        );
    }
}
