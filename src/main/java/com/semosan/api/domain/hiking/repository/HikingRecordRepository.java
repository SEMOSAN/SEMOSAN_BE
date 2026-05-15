package com.semosan.api.domain.hiking.repository;

import com.semosan.api.domain.hiking.entity.HikingRecord;
import com.semosan.api.domain.hiking.repository.projection.UserHikingRecordProjection;
import com.semosan.api.domain.hiking.repository.projection.UserHikingMountainRecordProjection;
import com.semosan.api.domain.hiking.repository.projection.UserHikingRecordSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HikingRecordRepository extends JpaRepository<HikingRecord, Long> {

    @Query(
            value = """
                    SELECT
                        hr.mountain_id AS mountainId,
                        m.name AS mountainName,
                        m.image_url AS imageUrl,
                        COUNT(hr.id) AS hikingCount,
                        MAX(hr.created_at) AS lastHikedAt
                    FROM hiking_records hr
                    JOIN hiking_members hm ON hm.hiking_record_id = hr.id
                    JOIN mountains m ON m.id = hr.mountain_id
                    WHERE hm.user_id = :userId
                    GROUP BY hr.mountain_id, m.name, m.image_url
                    ORDER BY MAX(hr.created_at) DESC, hr.mountain_id DESC
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT hr.mountain_id)
                    FROM hiking_records hr
                    JOIN hiking_members hm ON hm.hiking_record_id = hr.id
                    WHERE hm.user_id = :userId
                    """,
            nativeQuery = true
    )
    Page<UserHikingMountainRecordProjection> findUserHikingMountainRecordsByUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT
                        hr.id AS hikingRecordId,
                        m.id AS mountainId,
                        m.name AS mountainName,
                        c.id AS courseId,
                        c.name AS courseName,
                        COALESCE(hr.photo_report_image_url, hr.clive_image_url, m.image_url) AS imageUrl,
                        c.distance AS distance,
                        hr.duration AS duration,
                        hr.created_at AS hikedAt
                    FROM hiking_records hr
                    JOIN hiking_members hm ON hm.hiking_record_id = hr.id
                    JOIN mountains m ON m.id = hr.mountain_id
                    JOIN courses c ON c.id = hr.course_id
                    WHERE hm.user_id = :userId
                    ORDER BY hr.created_at DESC, hr.id DESC
            """,
            countQuery = """
                    SELECT COUNT(DISTINCT hr.id)
                    FROM hiking_records hr
                    JOIN hiking_members hm ON hm.hiking_record_id = hr.id
                    WHERE hm.user_id = :userId
                    """,
            nativeQuery = true
    )
    Page<UserHikingRecordProjection> findUserHikingRecordsByUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT
                        COUNT(hm.id) AS totalHikingCount,
                        COUNT(DISTINCT hr.mountain_id) AS conqueredMountainCount,
                        COALESCE(SUM(hr.max_altitude), 0) AS totalAltitude
                    FROM hiking_members hm
                    JOIN hiking_records hr ON hm.hiking_record_id = hr.id
                    WHERE hm.user_id = :userId
                    """,
            nativeQuery = true
    )
    UserHikingRecordSummaryProjection findUserHikingRecordSummaryByUserId(@Param("userId") Long userId);

    @Query(
            value = """
                    SELECT
                        hr.id AS hikingRecordId,
                        m.id AS mountainId,
                        m.name AS mountainName,
                        c.id AS courseId,
                        c.name AS courseName,
                        COALESCE(hr.photo_report_image_url, hr.clive_image_url, m.image_url) AS imageUrl,
                        c.distance AS distance,
                        hr.duration AS duration,
                        hr.created_at AS hikedAt
                    FROM hiking_records hr
                    JOIN hiking_members hm ON hm.hiking_record_id = hr.id
                    JOIN mountains m ON m.id = hr.mountain_id
                    JOIN courses c ON c.id = hr.course_id
                    WHERE hm.user_id = :userId AND hr.mountain_id = :mountainId
                    ORDER BY hr.created_at DESC, hr.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT hr.id)
                    FROM hiking_records hr
                    JOIN hiking_members hm ON hm.hiking_record_id = hr.id
                    WHERE hm.user_id = :userId AND hr.mountain_id = :mountainId
                    """,
            nativeQuery = true
    )
    Page<UserHikingRecordProjection> findUserHikingRecordsByUserIdAndMountainId(
            @Param("userId") Long userId,
            @Param("mountainId") Long mountainId,
            Pageable pageable
    );
}
