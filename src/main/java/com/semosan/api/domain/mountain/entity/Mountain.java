package com.semosan.api.domain.mountain.entity;

import com.semosan.api.common.base.BaseEntity;
import com.semosan.api.domain.mountain.enums.Difficulty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
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

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    /**
     * PostGIS 공간 좌표 컬럼 (SRID 4326 = WGS84).
     * latitude/longitude 값과 별도로, 공간 쿼리(nearest/거리 등) 용도로 사용.
     * 기존 데이터는 마이그레이션 스크립트로 채워넣음. (resources/db/migration 참고)
     */
    @Column(name = "location", columnDefinition = "geography(Point, 4326)")
    private Point location;
}
