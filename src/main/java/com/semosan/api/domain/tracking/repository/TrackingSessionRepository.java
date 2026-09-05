package com.semosan.api.domain.tracking.repository;

import com.semosan.api.domain.tracking.entity.TrackingSession;
import com.semosan.api.domain.tracking.enums.TrackingSessionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrackingSessionRepository extends JpaRepository<TrackingSession, Long> {

    /** 유저당 진행 중 세션이 이미 있는지 — 409 분기에 사용. */
    boolean existsByUser_IdAndStatusIn(Long userId, Collection<TrackingSessionStatus> statuses);

    /** 세션 소유권 확인 — STOMP SUBSCRIBE 인가에 사용. */
    boolean existsByIdAndUser_Id(Long id, Long userId);

    /** 유저의 활성(IN_PROGRESS/PAUSED) 세션 1건 조회 — 앱 재진입 시. */
    Optional<TrackingSession> findFirstByUser_IdAndStatusInOrderByStartedAtDesc(
            Long userId,
            Collection<TrackingSessionStatus> statuses
    );

    @Query("""
            SELECT ts FROM TrackingSession ts
            JOIN FETCH ts.user
            WHERE ts.id = :id
            """)
    Optional<TrackingSession> findByIdWithUser(@Param("id") Long id);

    @Query("""
            SELECT ts FROM TrackingSession ts
            JOIN FETCH ts.user
            JOIN FETCH ts.mountain
            LEFT JOIN FETCH ts.course
            WHERE ts.id = :id
            """)
    Optional<TrackingSession> findByIdWithRelations(@Param("id") Long id);

    @Query("""
            SELECT ts FROM TrackingSession ts
            JOIN FETCH ts.user
            JOIN FETCH ts.mountain
            LEFT JOIN FETCH ts.course
            WHERE ts.user.id = :userId AND ts.status IN :statuses
            ORDER BY ts.startedAt DESC
            LIMIT 1
            """)
    Optional<TrackingSession> findFirstActiveWithRelations(
            @Param("userId") Long userId,
            @Param("statuses") Collection<TrackingSessionStatus> statuses
    );

    /**
     * 24h 자동 만료 스케줄러용.
     * 활성 상태(IN_PROGRESS/PAUSED) 이면서 마지막 업데이트가 cutoff 이전인 세션을 찾는다.
     */
    @Query("""
            SELECT ts FROM TrackingSession ts
            WHERE ts.status IN :statuses
              AND ts.updatedAt < :cutoff
            """)
    List<TrackingSession> findStaleActiveSessions(
            @Param("statuses") Collection<TrackingSessionStatus> statuses,
            @Param("cutoff") LocalDateTime cutoff
    );

    /**
     * 고아 좌표 정리 스케줄러용 — 좌표를 지워도 되는 세션 ID 를 배치 단위로 가져온다.
     *
     * 조건:
     *  - ABANDONED (COMPLETED 는 HikingRecord 경로 폴리라인에 쓰이므로 제외)
     *  - endedAt 이 cutoff 이전 (유예 기간 경과)
     *  - HikingRecord 에 연결되지 않음 — 안전장치. 정상적으로 ABANDONED 는 기록이 없지만,
     *    데이터가 어긋난 경우에도 사용자에게 보이는 기록을 건드리지 않도록 방어한다.
     *  - 아직 좌표가 남아 있음 — 세션 행 자체는 남기는 정책이라, 이 조건이 없으면
     *    이미 정리된 세션이 매 실행마다 다시 조회되어 루프가 끝나지 않는다.
     */
    @Query("""
            SELECT ts.id FROM TrackingSession ts
            WHERE ts.status = :status
              AND ts.endedAt < :cutoff
              AND NOT EXISTS (SELECT 1 FROM HikingRecord hr WHERE hr.trackingSession = ts)
              AND EXISTS (SELECT 1 FROM TrackingPoint p WHERE p.trackingSession = ts)
            ORDER BY ts.id
            """)
    List<Long> findSessionIdsForPointCleanup(
            @Param("status") TrackingSessionStatus status,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable
    );
}
