package com.semosan.api.domain.mountain.repository;

import com.semosan.api.domain.mountain.entity.Mountain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MountainRepository extends JpaRepository<Mountain, Long> {

    @Query("SELECT m FROM Mountain m WHERE m.name LIKE CONCAT('%', :keyword, '%') OR m.address LIKE CONCAT('%', :keyword, '%')")
    Page<Mountain> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

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
                    ORDER BY location <-> ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
                    LIMIT 1
                    """,
            nativeQuery = true
    )
    Optional<Mountain> findNearestByLatLng(@Param("lat") Double lat, @Param("lng") Double lng);
}
