package com.semosan.api.domain.tracking.repository;

import com.semosan.api.domain.tracking.entity.TrackingPoint;
import com.semosan.api.domain.tracking.repository.projection.TrackingPathProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrackingPointRepository extends JpaRepository<TrackingPoint, Long> {

    List<TrackingPoint> findByTrackingSession_IdOrderByRecordedAtAsc(Long sessionId);

    /**
     * 세션의 GPS 점들을 시간순 LineString(GeoJSON) + 고도 배열(JSON) 로 묶어 한 번에 가져온다.
     * 점이 0~1개면 ST_MakeLine 이 null 을 반환해 track 이 null 로 응답된다.
     */
    @Query(
            value = """
                    SELECT
                        ST_AsGeoJSON(ST_MakeLine(location::geometry ORDER BY recorded_at))::text AS track,
                        json_agg(altitude ORDER BY recorded_at)::text                            AS altitudes
                    FROM tracking_points
                    WHERE tracking_session_id = :sessionId
                    """,
            nativeQuery = true
    )
    Optional<TrackingPathProjection> findTrackBySessionId(@Param("sessionId") Long sessionId);
}
