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

    /** 관리자가 waypoint 중에서 선택해 반영한 코스 정상 좌표. 미반영 시 null. */
    @Column(name = "summit_lat")
    private Double summitLat;

    @Column(name = "summit_lng")
    private Double summitLng;

    @Column(name = "summit_ele")
    private Double summitEle;

    /**
     * 관리자가 지도에서 좌표를 찍어 만든 코스.
     *
     * 시드 코스와 달리 고도 정보가 없다 — 지도 클릭으로는 위경도만 얻기 때문이다.
     * 그래서 altitudes/ascent/descent/maxAltitude/waypoints 는 모두 null 로 둔다.
     * altitudes 가 null 이면 CourseSlopeSegmentCalculator 가 빈 세그먼트를 돌려주므로
     * 경사도 그래프만 비고 코스 상세 자체는 정상 동작한다.
     *
     * 정상 좌표는 생성 후 {@link #updateSummit} 로 따로 채운다. 정상이 없으면
     * 사진 마일스톤이 정상 기준이 아니라 distance 4등분으로 fallback 된다.
     *
     * @param polyline SRID 4326 으로 만든 LineString. SRID 를 빠뜨리면 geography 컬럼과
     *                 어긋나 이후 공간 쿼리가 깨진다.
     */
    public static Course create(Mountain mountain, String name, Difficulty difficulty,
                                Double distance, Integer duration, LineString polyline,
                                String startName, String endName) {
        return Course.builder()
                .mountain(mountain)
                .name(name)
                .difficulty(difficulty)
                .distance(distance)
                .duration(duration)
                .polyline(polyline)
                .startName(startName)
                .endName(endName)
                .build();
    }

    /** 선택한 waypoint 를 그대로 스냅샷 — 고도 없는 waypoint 면 summitEle 도 null 로 덮어쓴다. */
    public void updateSummit(Double summitLat, Double summitLng, Double summitEle) {
        this.summitLat = summitLat;
        this.summitLng = summitLng;
        this.summitEle = summitEle;
    }

    public record CourseWaypoint(
            Double lat,
            Double lng,
            Double ele,
            String name,
            String category
    ) {
    }
}
