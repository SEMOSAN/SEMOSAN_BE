package com.semosan.api.domain.mountain.repository;

import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.repository.projection.CourseDetailProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByMountainId(Long mountainId);

    @Query("""
            SELECT c
            FROM Course c
            JOIN FETCH c.mountain m
            WHERE m.isPublic = true
            ORDER BY m.id ASC, c.id ASC
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

    /**
     * 주어진 산 ID 목록에 대해 각 산별 코스 수를 일괄 조회한다.
     * 결과는 Object[] 배열로 반환되며, [0] = mountainId (Long), [1] = count (Long).
     */
    @Query("SELECT c.mountain.id, COUNT(c) FROM Course c WHERE c.mountain.id IN :mountainIds GROUP BY c.mountain.id")
    List<Object[]> countByMountainIds(@Param("mountainIds") Collection<Long> mountainIds);
}
