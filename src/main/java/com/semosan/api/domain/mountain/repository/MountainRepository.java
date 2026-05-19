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

public interface MountainRepository extends JpaRepository<Mountain, Long> {

    @Query("SELECT m FROM Mountain m WHERE m.name LIKE CONCAT('%', :keyword, '%') OR m.address LIKE CONCAT('%', :keyword, '%')")
    Page<Mountain> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

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
                    ORDER BY (POWER(latitude - :lat, 2) + POWER(longitude - :lng, 2)) ASC, id ASC
                    """,
            countQuery = """
                    SELECT COUNT(*) FROM mountains
                    WHERE difficulty IN (:difficulties)
                      AND latitude IS NOT NULL
                      AND longitude IS NOT NULL
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
                        (
                            SELECT COUNT(hr.id)
                            FROM hiking_records hr
                            JOIN hiking_members hm ON hm.hiking_record_id = hr.id
                            WHERE hr.mountain_id = m.id AND hm.user_id = :userId
                        ) AS visitCount,
                        (
                            SELECT COALESCE(hr.photo_report_image_url, hr.clive_image_url)
                            FROM hiking_records hr
                            JOIN hiking_members hm ON hm.hiking_record_id = hr.id
                            WHERE hr.mountain_id = m.id AND hm.user_id = :userId
                            ORDER BY hr.created_at DESC
                            LIMIT 1
                        ) AS imageUrl
                    FROM mountains m
                    WHERE m.latitude BETWEEN :swLat AND :neLat
                      AND m.longitude BETWEEN :swLng AND :neLng
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
}
