package com.semosan.api.domain.mountain.repository;

import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.repository.projection.CourseDetailProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByMountainId(Long mountainId);

    @Query("""
            SELECT c
            FROM Course c
            JOIN FETCH c.mountain
            ORDER BY c.mountain.id ASC, c.id ASC
            """)
    List<Course> findAllWithMountainForRecommendation();

    /**
     * 코스 상세 — polyline 은 PostGIS LineString 을 GeoJSON 으로 변환,
     * altitudes 는 jsonb 를 그대로 문자열로 반환. 클라이언트 응답 시 raw JSON 으로 직렬화한다.
     */
    @Query(
            value = """
                    SELECT
                        c.id            AS id,
                        c.mountain_id   AS mountainId,
                        c.name          AS name,
                        c.difficulty    AS difficulty,
                        c.distance      AS distance,
                        c.duration      AS duration,
                        c.start_name    AS startName,
                        c.end_name      AS endName,
                        c.ascent        AS ascent,
                        c.descent       AS descent,
                        c.max_altitude  AS maxAltitude,
                        ST_AsGeoJSON(c.polyline)::text AS polyline,
                        c.altitudes::text              AS altitudes
                    FROM courses c
                    WHERE c.id = :courseId
                    """,
            nativeQuery = true
    )
    Optional<CourseDetailProjection> findCourseDetailById(@Param("courseId") Long courseId);
}
