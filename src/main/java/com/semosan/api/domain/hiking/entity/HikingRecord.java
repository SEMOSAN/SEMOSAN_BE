package com.semosan.api.domain.hiking.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.Mountain;
import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /** 실제 소요 시간 (초) */
    @Column(name = "duration", nullable = false)
    private Integer duration;

    /** TODO: 등산 중 도달한 최고 고도 (m)가 맞는지 확인 필요 */
    @Column(name = "max_altitude", nullable = false)
    private Double altitude;

    /** 소모 칼로리 (kcal) */
    @Column(name = "calories", nullable = false)
    private Integer calories;

    @Column(name = "clive_image_url", length = 500)
    private String cliveImageUrl;

    @Column(name = "photo_report_image_url", length = 500)
    private String photoReportImageUrl;

    public static HikingRecord create(
            Mountain mountain,
            Course course,
            Integer duration,
            Double altitude,
            Integer calories,
            String cliveImageUrl,
            String photoReportImageUrl
    ) {
        return HikingRecord.builder()
                .mountain(mountain)
                .course(course)
                .duration(duration)
                .altitude(altitude)
                .calories(calories)
                .cliveImageUrl(cliveImageUrl)
                .photoReportImageUrl(photoReportImageUrl)
                .build();
    }
}
