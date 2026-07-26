package com.semosan.api.domain.tracking.repository;

import com.semosan.api.domain.tracking.entity.TrackingSession;
import com.semosan.api.domain.tracking.enums.TrackingSessionStatus;
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
}
