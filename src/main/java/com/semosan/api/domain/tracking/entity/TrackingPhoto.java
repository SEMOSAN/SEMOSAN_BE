package com.semosan.api.domain.tracking.entity;

import com.semosan.api.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 트래킹 진행 중 마일스톤 도달 시 사용자가 촬영해 업로드한 사진의 메타.
 * 실제 이미지 바이너리는 MinIO 에 보관되고 본 테이블엔 URL 만 저장.
 * 한 트래킹 세션이 N 개의 사진(=마일스톤 수)을 가질 수 있다 — N 의 상한은 정책상 6.
 */
@Table(
        name = "tracking_photos",
        indexes = {
                @Index(name = "idx_tracking_photos_session", columnList = "tracking_session_id"),
                @Index(name = "idx_tracking_photos_session_milestone",
                        columnList = "tracking_session_id, milestone_index")
        }
)
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackingPhoto extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracking_session_id", nullable = false)
    private TrackingSession trackingSession;

    /** 이 사진이 어떤 마일스톤에 해당하는지 (0-based, 코스 4컷 / 자유 최대 6컷). */
    @Column(name = "milestone_index", nullable = false)
    private Integer milestoneIndex;

    /** 마일스톤의 기준 거리 (m). 예: 500, 1000, ... 또는 코스의 distance/4 등. */
    @Column(name = "milestone_distance_m", nullable = false)
    private Double milestoneDistanceM;

    /** MinIO 등 외부 스토리지에 업로드된 이미지 URL (presigned URL 발급은 #41). */
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    @Column(name = "lat", nullable = false)
    private Double lat;

    @Column(name = "lng", nullable = false)
    private Double lng;

    /** 촬영 시점 디바이스가 보고한 고도 (nullable). */
    @Column(name = "altitude")
    private Double altitude;

    public static TrackingPhoto create(
            TrackingSession session,
            Integer milestoneIndex,
            Double milestoneDistanceM,
            String imageUrl,
            LocalDateTime capturedAt,
            Double lat,
            Double lng,
            Double altitude
    ) {
        return TrackingPhoto.builder()
                .trackingSession(session)
                .milestoneIndex(milestoneIndex)
                .milestoneDistanceM(milestoneDistanceM)
                .imageUrl(imageUrl)
                .capturedAt(capturedAt)
                .lat(lat)
                .lng(lng)
                .altitude(altitude)
                .build();
    }
}
