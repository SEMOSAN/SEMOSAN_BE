package com.semosan.api.domain.mountain.repository;

import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.repository.projection.MountainMapProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MountainRepository extends JpaRepository<Mountain, Long> {

    @Query("SELECT m FROM Mountain m WHERE m.isPublic = true AND (m.name LIKE CONCAT('%', :keyword, '%') OR m.address LIKE CONCAT('%', :keyword, '%'))")
    Page<Mountain> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    Page<Mountain> findByIsPublicTrue(Pageable pageable);

    /**
     * 주어진 좌표(lat, lng)에서 가장 가까운 산 1개를 PostGIS 거리 연산자로 조회한다.
     * `<->` : PostGIS 거리 연산자, GIST 인덱스(idx_mountains_location)로 가속됨.
     * 임계 거리 제한 없음 — 정책상 "그래도 가장 가까운 산 반환".
     * location 컬럼이 null 인 산은 제외.
     */
    @Query(
            value = """
                    SELECT * FROM mountains
                    WHERE location IS NOT NULL
                      AND is_public = true
                    ORDER BY location <-> ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
                    LIMIT 1
                    """,
            nativeQuery = true
    )
    Optional<Mountain> findNearestByLatLng(@Param("lat") Double lat, @Param("lng") Double lng);

    /**
     * 주어진 난이도 집합에 속하는 산을 사용자 위치에서 가까운 순으로 페이징 조회한다.
     *
     * 정렬 기준 — squared distance:
     *   (lat - :lat)^2 + (lng - :lng)^2
     *   - 진짜 거리(m) 가 아니라 비례 제곱값. trig/sqrt 생략 → 빠름.
     *   - 한국 영역 안에서는 위/경도 1도의 미터 차이가 작아 squared distance 의 정렬 순서가
     *     실제 거리(Haversine) 순서와 동일하다. 운영 영역이 더 넓어지면 Haversine 으로 교체 검토.
     * 같은 거리일 때의 보조 정렬 — id ASC: 페이지 간 일관성 보장.
     * 좌표 누락 산(latitude/longitude null) 은 제외.
     *
     * 본 native query 는 ORDER BY 가 박혀 있어 Pageable.sort 는 무시됨 (서버 정책 우선).
     */
    @Query(
            value = """
                    SELECT * FROM mountains
                    WHERE difficulty IN (:difficulties)
                      AND latitude IS NOT NULL
                      AND longitude IS NOT NULL
                      AND is_public = true
                    ORDER BY (POWER(latitude - :lat, 2) + POWER((longitude - :lng) * COS(RADIANS(:lat)), 2)) ASC, id ASC
                    """,
            countQuery = """
                    SELECT COUNT(*) FROM mountains
                    WHERE difficulty IN (:difficulties)
                      AND latitude IS NOT NULL
                      AND longitude IS NOT NULL
                      AND is_public = true
                    """,
            nativeQuery = true
    )
    Page<Mountain> findRecommendationsByDifficulties(
            @Param("difficulties") Collection<String> difficulties,
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT
                        m.id AS id,
                        m.name AS name,
                        m.latitude AS latitude,
                        m.longitude AS longitude,
                        COUNT(hm.user_id) AS visitCount,
                        m.image_urls->>0 AS imageUrl
                    FROM mountains m
                    LEFT JOIN hiking_records hr ON hr.mountain_id = m.id
                    LEFT JOIN hiking_members hm ON hm.hiking_record_id = hr.id AND hm.user_id = :userId
                    WHERE m.latitude BETWEEN :swLat AND :neLat
                      AND m.longitude BETWEEN :swLng AND :neLng
                      AND m.is_public = true
                    GROUP BY m.id
                    ORDER BY m.id
                    """,
            nativeQuery = true
    )
    List<MountainMapProjection> findInBBoxWithUserHikingStats(
            @Param("userId") Long userId,
            @Param("swLat") Double swLat,
            @Param("swLng") Double swLng,
            @Param("neLat") Double neLat,
            @Param("neLng") Double neLng
    );

    /**
     * 관리자용 — isPublic 필터 없이 이름 또는 주소로 검색한다.
     */
    @Query("SELECT m FROM Mountain m WHERE m.name LIKE CONCAT('%', :keyword, '%') OR m.address LIKE CONCAT('%', :keyword, '%')")
    Page<Mountain> searchByKeywordAll(@Param("keyword") String keyword, Pageable pageable);

    Page<Mountain> findByIsPublicFalse(Pageable pageable);

    @Query("SELECT m FROM Mountain m WHERE m.isPublic = :isPublic AND (m.name LIKE CONCAT('%', :keyword, '%') OR m.address LIKE CONCAT('%', :keyword, '%'))")
    Page<Mountain> searchByKeywordAndVisibility(@Param("keyword") String keyword, @Param("isPublic") boolean isPublic, Pageable pageable);
}
