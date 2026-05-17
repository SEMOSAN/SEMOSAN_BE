package com.semosan.api.domain.hiking.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Table(name = "hiking_records")
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HikingRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mountain_id", nullable = false)
    private Mountain mountain;

    /** 자유 기록 또는 매뉴얼 입력 시 null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    /**
     * 트래킹 흐름으로 생성된 기록은 원본 세션을 참조.
     * 외부에서 수동 생성된 기록은 null 가능.
     * tracking_points 조회용: HikingRecord → trackingSession.id → tracking_points
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracking_session_id")
    private TrackingSession trackingSession;

    /** 실제 소요 시간 (초). 일시정지 시간 제외. */
    @Column(name = "duration", nullable = false)
    private Integer duration;

    /** 등산 중 도달한 최고 고도 (m). */
    @Column(name = "max_altitude", nullable = false)
    private Double altitude;

    /**
     * 소모 칼로리 (kcal).
     * TODO: 실제 추정 공식(distance/ascent/사용자 체중 등 기반)이 정해지면 산정 로직 추가.
     */
    @Column(name = "calories", nullable = false)
    private Integer calories;

    /** 트래킹 측정 거리 (m). 수동 기록은 null 가능. */
    @Column(name = "distance")
    private Double distance;

    /** 누적 상승 고도 (m). */
    @Column(name = "ascent")
    private Double ascent;

    /** 누적 하강 고도 (m). */
    @Column(name = "descent")
    private Double descent;

    /** 일시정지 누적 시간 (초). duration 에선 이미 제외된 값이고, 본 컬럼은 감사용. */
    @Column(name = "paused_seconds_total", nullable = false)
    private Integer pausedSecondsTotal;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "clive_image_url", length = 500)
    private String cliveImageUrl;

    @Column(name = "photo_report_image_url", length = 500)
    private String photoReportImageUrl;

    /**
     * 트래킹 세션 종료 시점의 통계를 영구 기록으로 변환한다.
     * 이미지(clive/photoReport) URL 은 추후 #46 사진 흐름에서 채워진다.
     */
    public static HikingRecord fromTrackingSession(
            TrackingSession session,
            Double distance,
            Double maxAltitude,
            Double ascent,
            Double descent
    ) {
        int duration = computeDurationSeconds(session);
        return HikingRecord.builder()
                .mountain(session.getMountain())
                .course(session.getCourse())
                .trackingSession(session)
                .duration(duration)
                .altitude(maxAltitude == null ? 0.0 : maxAltitude)
                .calories(0)  // TODO: 칼로리 산정 공식 도입 시 교체
                .distance(distance)
                .ascent(ascent)
                .descent(descent)
                .pausedSecondsTotal(session.getPausedSecondsTotal() == null ? 0 : session.getPausedSecondsTotal())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .build();
    }

    private static int computeDurationSeconds(TrackingSession session) {
        if (session.getStartedAt() == null || session.getEndedAt() == null) {
            return 0;
        }
        long totalSeconds = java.time.Duration.between(session.getStartedAt(), session.getEndedAt()).toSeconds();
        int paused = session.getPausedSecondsTotal() == null ? 0 : session.getPausedSecondsTotal();
        long net = totalSeconds - paused;
        return (int) Math.max(net, 0);
    }
}
