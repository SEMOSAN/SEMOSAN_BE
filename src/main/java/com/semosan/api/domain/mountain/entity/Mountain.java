package com.semosan.api.domain.mountain.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.mountain.enums.Difficulty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.util.List;

@Table(name = "mountains")
@Getter
@Entity
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mountain extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "altitude", nullable = false)
    private Double altitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private Difficulty difficulty;

    @Column(name = "duration")
    private Integer duration;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_urls", columnDefinition = "jsonb")
    private List<String> imageUrls;

    /**
     * 좌표는 두 가지 표현으로 중복 저장한다 — 단순 직렬화/BETWEEN 용 latitude/longitude,
     * PostGIS 공간 쿼리(nearest/거리 등) 용 location.
     * 응답 DTO 가 latitude/longitude 를 그대로 노출하고 있어 location 단일화는 별도 PR 에서 다룬다.
     * 좌표 변경 시 반드시 {@link #updateCoordinates(Double, Double)} 로 세 컬럼을 함께 갱신할 것.
     */
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "location", columnDefinition = "geography(Point, 4326)")
    private Point location;

    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = true;

    public void updateInfo(String name, String address, Double altitude, Difficulty difficulty, Integer duration) {
        this.name = name;
        this.address = address;
        this.altitude = altitude;
        this.difficulty = difficulty;
        this.duration = duration;
    }

    public void updateImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public void updateVisibility(boolean isPublic) {
        this.isPublic = isPublic;
    }

    /** 정상 waypoint 반영: 좌표를 갱신하고, 고도가 주어지면 함께 갱신한다. */
    public void updateSummit(Double latitude, Double longitude, Double altitude) {
        updateCoordinates(latitude, longitude);
        if (altitude != null) {
            this.altitude = altitude;
        }
    }

    public void updateCoordinates(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        // PostGIS Point 는 (X=경도, Y=위도) 순서
        this.location = new GeometryFactory(new PrecisionModel(), 4326)
                .createPoint(new Coordinate(longitude, latitude));
    }
}
