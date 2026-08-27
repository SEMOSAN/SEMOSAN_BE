package com.semosan.api.domain.tracking.entity;

import com.semosan.api.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Table(
        name = "tracking_points",
        indexes = {
                @Index(name = "idx_tracking_points_session_recorded", columnList = "tracking_session_id, recorded_at")
        }
)
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackingPoint extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracking_session_id", nullable = false)
    private TrackingSession trackingSession;

    /**
     * 좌표 (PostGIS geography(Point, 4326)).
     * Haversine 직접 계산보다 ST_Distance(geography) 가 정확 (구 좌표계 기반).
     */
    @Column(name = "location", columnDefinition = "geography(Point, 4326)", nullable = false)
    private Point location;

    /** 디바이스가 보고한 고도(m). null 가능 — 일부 디바이스/실내에선 누락. */
    @Column(name = "altitude")
    private Double altitude;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    public static TrackingPoint create(TrackingSession session, Point location, Double altitude, LocalDateTime recordedAt) {
        return TrackingPoint.builder()
                .trackingSession(session)
                .location(location)
                .altitude(altitude)
                .recordedAt(recordedAt)
                .build();
    }
}
