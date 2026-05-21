package com.semosan.api.domain.tracking.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.tracking.enums.TrackingSessionStatus;
import com.semosan.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Table(name = "tracking_sessions")
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackingSession extends BaseEntity {

    private static final Set<TrackingSessionStatus> TERMINAL_STATES =
            EnumSet.of(TrackingSessionStatus.COMPLETED, TrackingSessionStatus.ABANDONED);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mountain_id", nullable = false)
    private Mountain mountain;

    /** 자유 기록 시 null */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "is_free_recording", nullable = false)
    private Boolean isFreeRecording;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TrackingSessionStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /** 현재 일시정지 중인 경우의 시작 시각. resume 시 누적 후 null 로 리셋. */
    @Column(name = "paused_at")
    private LocalDateTime pausedAt;

    /** 누적 일시정지 시간 (초). duration 계산 시 제외. */
    @Column(name = "paused_seconds_total", nullable = false)
    private Integer pausedSecondsTotal;

    public static TrackingSession create(User user, Mountain mountain, Course course, boolean isFreeRecording) {
        return TrackingSession.builder()
                .user(user)
                .mountain(mountain)
                .course(course)
                .isFreeRecording(isFreeRecording)
                .status(TrackingSessionStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .pausedSecondsTotal(0)
                .build();
    }

    public void pause() {
        if (status != TrackingSessionStatus.IN_PROGRESS) {
            throw new GeneralException(ErrorStatus.TRACKING_SESSION_INVALID_STATE);
        }
        this.status = TrackingSessionStatus.PAUSED;
        this.pausedAt = LocalDateTime.now();
    }

    public void resume() {
        if (status != TrackingSessionStatus.PAUSED) {
            throw new GeneralException(ErrorStatus.TRACKING_SESSION_INVALID_STATE);
        }
        if (pausedAt != null) {
            int elapsed = (int) Duration.between(pausedAt, LocalDateTime.now()).toSeconds();
            this.pausedSecondsTotal = pausedSecondsTotal + Math.max(elapsed, 0);
            this.pausedAt = null;
        }
        this.status = TrackingSessionStatus.IN_PROGRESS;
    }

    public void complete() {
        if (TERMINAL_STATES.contains(status)) {
            throw new GeneralException(ErrorStatus.TRACKING_SESSION_INVALID_STATE);
        }
        finalizePausedSeconds();
        this.status = TrackingSessionStatus.COMPLETED;
        this.endedAt = LocalDateTime.now();
    }

    public void abandon() {
        if (TERMINAL_STATES.contains(status)) {
            throw new GeneralException(ErrorStatus.TRACKING_SESSION_INVALID_STATE);
        }
        finalizePausedSeconds();
        this.status = TrackingSessionStatus.ABANDONED;
        this.endedAt = LocalDateTime.now();
    }

    /**
     * 종료 시점에 만약 일시정지 중이었다면 그 구간을 누적에 반영한 뒤 pausedAt 리셋.
     */
    private void finalizePausedSeconds() {
        if (status == TrackingSessionStatus.PAUSED && pausedAt != null) {
            int elapsed = (int) Duration.between(pausedAt, LocalDateTime.now()).toSeconds();
            this.pausedSecondsTotal = pausedSecondsTotal + Math.max(elapsed, 0);
            this.pausedAt = null;
        }
    }

    public boolean isOwnedBy(Long userId) {
        return user != null && userId != null && userId.equals(user.getId());
    }
}
