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
     * 주어진 난이도 집합에 속하는 산을 랜덤 순서로 페이징 조회한다.
     * TODO: ORDER BY RANDOM() + OFFSET 조합은 페이지 간 중복/누락 가능성이 있음.
     *       팀 추천 정책 확정 시 정식 정렬 로직(인기/거리/고도/시드 기반 등)으로 교체.
     */
    @Query(
            value = """
                    SELECT * FROM mountains
                    WHERE difficulty IN (:difficulties)
                    ORDER BY RANDOM()
                    """,
            countQuery = """
                    SELECT COUNT(*) FROM mountains
                    WHERE difficulty IN (:difficulties)
                    """,
            nativeQuery = true
    )
    Page<Mountain> findRecommendationsByDifficulties(
            @Param("difficulties") Collection<String> difficulties,
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
