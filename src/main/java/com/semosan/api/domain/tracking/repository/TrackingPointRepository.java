package com.semosan.api.domain.tracking.repository;

import com.semosan.api.domain.tracking.entity.TrackingPoint;
import com.semosan.api.domain.tracking.repository.projection.TrackingPathProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * 여러 세션의 좌표를 한 번의 DELETE 로 제거한다 — 고아 좌표 정리용.
     *
     * 파생 메서드(deleteByTrackingSession_Id)를 쓰지 않는 이유:
     * JPA 가 대상 엔티티를 전부 SELECT 로 로드한 뒤 건당 DELETE 를 날린다.
     * 세션 하나가 좌표 1,000 건대를 쌓으므로 벌크 삭제여야 한다.
     */
    @Modifying
    @Query("DELETE FROM TrackingPoint p WHERE p.trackingSession.id IN :sessionIds")
    int deleteByTrackingSessionIdIn(@Param("sessionIds") List<Long> sessionIds);
}
