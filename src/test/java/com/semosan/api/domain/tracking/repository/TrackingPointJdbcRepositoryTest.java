package com.semosan.api.domain.tracking.repository;

import com.semosan.api.common.config.FirebaseConfig;
import com.semosan.api.domain.tracking.entity.TrackingPoint;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import com.semosan.api.domain.tracking.service.TrackingPointFlushService.PendingPoint;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TrackingPointJdbcRepositoryTest {

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    @MockitoBean
    private FirebaseConfig firebaseConfig;

    @Autowired
    private TrackingPointJdbcRepository trackingPointJdbcRepository;

    @Autowired
    private TrackingPointRepository trackingPointRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    @Test
    @DisplayName("saveAllInBatch 가 1회 배치 쿼리로 좌표 및 메타데이터를 정상 삽입한다")
    void saveAllInBatchInsertsPointsCorrectly() {
        Long userId = insertUser();
        Long mountainId = insertMountain();
        Long sessionId = insertTrackingSession(userId, mountainId);

        LocalDateTime now = LocalDateTime.now();
        List<PendingPoint> points = List.of(
                new PendingPoint(37.5665, 126.9780, 150.0, now.minusSeconds(10)),
                new PendingPoint(37.5666, 126.9781, 155.0, now.minusSeconds(5)),
                new PendingPoint(37.5667, 126.9782, null, now) // altitude null 케이스
        );

        int savedCount = trackingPointJdbcRepository.saveAllInBatch(sessionId, points, now);

        assertThat(savedCount).isEqualTo(3);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT
                    tracking_session_id,
                    ST_Y(location::geometry) AS lat,
                    ST_X(location::geometry) AS lng,
                    altitude,
                    recorded_at
                FROM tracking_points
                WHERE tracking_session_id = ?
                ORDER BY recorded_at ASC
                """, sessionId);

        assertThat(rows).hasSize(3);
        assertThat(((Number) rows.get(0).get("lat")).doubleValue()).isEqualTo(37.5665);
        assertThat(((Number) rows.get(0).get("lng")).doubleValue()).isEqualTo(126.9780);
        assertThat(((Number) rows.get(0).get("altitude")).doubleValue()).isEqualTo(150.0);

        assertThat(rows.get(2).get("altitude")).isNull();
    }

    /**
     * 로컬 성능 확인용 — CI 에서는 돌리지 않는다 (10000건 Before 케이스는 개별 INSERT 100회 이상 왕복이라 수십 초 소요).
     * 필요할 때 @Disabled 를 잠깐 지우고 로컬에서 직접 실행.
     */
    @Disabled("로컬 성능 확인용 벤치마크 — CI 대상 아님")
    @ParameterizedTest(name = "[Before vs After 벤치마크] {0}건 좌표 저장 시 JPA saveAll vs JDBC batchUpdate 비교")
    @ValueSource(ints = {100, 1_000, 10_000})
    void benchmarkBeforeVsAfter(int pointCount) {
        Long userIdBefore = insertUser();
        Long userIdAfter = insertUser();
        Long mountainId = insertMountain();
        Long sessionIdBefore = insertTrackingSession(userIdBefore, mountainId);
        Long sessionIdAfter = insertTrackingSession(userIdAfter, mountainId);

        LocalDateTime now = LocalDateTime.now();
        List<PendingPoint> points = new ArrayList<>(pointCount);
        for (int i = 0; i < pointCount; i++) {
            points.add(new PendingPoint(37.5 + (i * 0.0001), 127.0 + (i * 0.0001), 100.0 + i, now.minusSeconds(pointCount - i)));
        }

        // ----------------------------------------------------
        // 1. Before: JPA saveAll (GenerationType.IDENTITY -> pointCount 회 개별 INSERT)
        // ----------------------------------------------------
        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        TrackingSession sessionProxy = entityManager.getReference(TrackingSession.class, sessionIdBefore);
        List<TrackingPoint> entities = points.stream().map(p -> {
            Point loc = GEOMETRY_FACTORY.createPoint(new Coordinate(p.lng(), p.lat()));
            loc.setSRID(4326);
            return TrackingPoint.create(sessionProxy, loc, p.altitude(), p.recordedAt());
        }).toList();

        long beforeStart = System.nanoTime();
        trackingPointRepository.saveAll(entities);
        entityManager.flush();
        long beforeEnd = System.nanoTime();
        double beforeDurationMs = (beforeEnd - beforeStart) / 1_000_000.0;
        long beforeQueryCount = statistics.getPrepareStatementCount();

        // ----------------------------------------------------
        // 2. After: JDBC batchUpdate (1회 배치 INSERT)
        // ----------------------------------------------------
        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        long afterStart = System.nanoTime();
        int afterSaved = trackingPointJdbcRepository.saveAllInBatch(sessionIdAfter, points, now);
        long afterEnd = System.nanoTime();
        double afterDurationMs = (afterEnd - afterStart) / 1_000_000.0;

        // ----------------------------------------------------
        // 3. 결과 출력 및 검증
        // ----------------------------------------------------
        System.out.println("\n=================================================");
        System.out.printf("📊 [트래킹 좌표 %,d건 저장 벤치마크 결과 비교]%n", pointCount);
        System.out.println("-------------------------------------------------");
        System.out.printf("• Before (JPA saveAll)       : %d 회 INSERT 실행 | 소요 시간: %.2f ms\n", beforeQueryCount, beforeDurationMs);
        System.out.printf("• After  (JDBC batchUpdate)  :  1 회 배치 INSERT 실행 | 소요 시간: %.2f ms\n", afterDurationMs);
        if (beforeDurationMs > 0) {
            double speedup = ((beforeDurationMs - afterDurationMs) / beforeDurationMs) * 100.0;
            System.out.printf("• 속도 개선율: %.1f%%\n", speedup);
        }
        System.out.println("=================================================\n");

        assertThat(beforeQueryCount).isEqualTo(pointCount); // JPA IDENTITY 전략으로 인해 개별 INSERT
        assertThat(afterSaved).isEqualTo(pointCount);
    }

    private Long insertUser() {
        String oauthId = "tracking-point-test-" + System.nanoTime();
        return jdbcTemplate.queryForObject("""
                INSERT INTO users (
                    created_at, updated_at, device_type, onboarding_status, oauth_id, oauth_provider,
                    is_deleted, name
                )
                VALUES (now(), now(), 'IOS', 'INCOMPLETE', ?, 'TEST', false, '테스터')
                RETURNING id
                """, Long.class, oauthId);
    }

    private Long insertMountain() {
        return jdbcTemplate.queryForObject("""
                INSERT INTO mountains (
                    created_at, updated_at, name, address, altitude, difficulty, duration,
                    image_urls, latitude, longitude, is_public
                )
                VALUES (
                    now(), now(), '트래킹산', '서울 테스트구', 500.0, 'NORMAL', 90,
                    '[]'::jsonb, 37.5, 127.0, true
                )
                RETURNING id
                """, Long.class);
    }

    private Long insertTrackingSession(Long userId, Long mountainId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO tracking_sessions (
                    created_at, updated_at, user_id, mountain_id, is_free_recording, status, started_at, paused_seconds_total
                )
                VALUES (now(), now(), ?, ?, true, 'IN_PROGRESS', now(), 0)
                RETURNING id
                """, Long.class, userId, mountainId);
    }
}
