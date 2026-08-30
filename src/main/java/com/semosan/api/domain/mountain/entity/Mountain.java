package com.semosan.api.domain.mountain.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.mountain.enums.Difficulty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

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
     * 좌표 변경은 {@link #updateCoordinates(Double, Double)} 로 한다 — location 은 따라온다.
     */
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    /**
     * DB 생성 컬럼(V39) — latitude/longitude 에서 자동 파생되므로 애플리케이션은 쓰지 않는다.
     * INSERT/UPDATE 직후 DB 가 계산한 값을 다시 읽어와 메모리 상태를 맞춘다.
     */
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "location", columnDefinition = "geography(Point, 4326)",
            insertable = false, updatable = false)
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

    /**
     * location 은 DB 생성 컬럼이라 여기서 건드리지 않는다 — flush 시점에 DB 가 계산해 준다.
     */
    public void updateCoordinates(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
