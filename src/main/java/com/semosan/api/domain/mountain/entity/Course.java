package com.semosan.api.domain.mountain.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.mountain.enums.Difficulty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.LineString;

import java.util.List;

@Table(name = "courses")
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mountain_id", nullable = false)
    private Mountain mountain;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private Difficulty difficulty;

    /** 코스 총 거리. 단위: 미터(m). 시드/응답/마일스톤 계산 모두 m 기준 일관. */
    @Column(name = "distance", nullable = false)
    private Double distance;

    /** 코스 소요 시간. 단위: 분. */
    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Column(name = "start_name", length = 100)
    private String startName;

    @Column(name = "end_name", length = 100)
    private String endName;

    /** 누적 상승 고도(m). Σ max(0, current - previous). altitudes 없거나 점 1개 이하면 null. */
    @Column(name = "ascent")
    private Double ascent;

    /** 누적 하강 고도(m). Σ max(0, previous - current). altitudes 없거나 점 1개 이하면 null. */
    @Column(name = "descent")
    private Double descent;

    /** 코스 최고 고도(m). altitudes 의 max. null 가능. */
    @Column(name = "max_altitude")
    private Double maxAltitude;

    @Column(name = "polyline", columnDefinition = "geography(LineString, 4326)")
    private LineString polyline;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "altitudes", columnDefinition = "jsonb")
    private List<Double> altitudes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "waypoints", columnDefinition = "jsonb")
    private List<CourseWaypoint> waypoints;

    public record CourseWaypoint(
            Double lat,
            Double lng,
            Double ele,
            String name,
            String category
    ) {
    }
}
