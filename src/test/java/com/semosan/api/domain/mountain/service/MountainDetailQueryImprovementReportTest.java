package com.semosan.api.domain.mountain.service;

import com.semosan.api.common.config.FirebaseConfig;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "QUERY_IMPROVEMENT_REPORT", matches = "true")
class MountainDetailQueryImprovementReportTest {

    private static final Logger log = LoggerFactory.getLogger(MountainDetailQueryImprovementReportTest.class);
    // Measured on the pre-optimization path using the same local dataset and command.
    private static final long BEFORE_STATEMENTS = 7;
    private static final double BEFORE_AVG_MS = 29.5;
    private static final long BEFORE_MIN_MS = 19;
    private static final long BEFORE_MAX_MS = 46;

    @MockitoBean
    private FirebaseConfig firebaseConfig;

    @Autowired
    private MountainService mountainService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void measureMountainDetailQueryImprovement() {
        Long mountainId = resolveMountainId();
        Long userId = createTemporaryUser();
        try {
            Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
            statistics.setStatisticsEnabled(true);

            mountainService.getMountainDetail(userId, mountainId);

            List<Long> statementCounts = new ArrayList<>();
            List<Long> hibernateStatementCounts = new ArrayList<>();
            List<Long> elapsedMs = new ArrayList<>();
            MountainDetailResponse lastResponse = null;

            for (int i = 0; i < 10; i++) {
                statistics.clear();
                long startedAt = System.nanoTime();
                lastResponse = mountainService.getMountainDetail(userId, mountainId);
                long elapsed = (System.nanoTime() - startedAt) / 1_000_000;

                long hibernateStatementCount = statistics.getPrepareStatementCount();
                hibernateStatementCounts.add(hibernateStatementCount);
                statementCounts.add(hibernateStatementCount + 1);
                elapsedMs.add(elapsed);
            }

            assertThat(lastResponse).isNotNull();
            printImprovementReport(mountainId, statementCounts, hibernateStatementCounts, elapsedMs, lastResponse);
        } finally {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        }
    }

    private Long resolveMountainId() {
        Long requestedMountainId = parseLong(System.getenv("QUERY_IMPROVEMENT_REPORT_MOUNTAIN_ID"));
        if (requestedMountainId != null) {
            return requestedMountainId;
        }

        return jdbcTemplate.queryForObject("""
                SELECT m.id
                FROM mountains m
                WHERE m.is_public = true
                ORDER BY
                    (SELECT COUNT(*) FROM courses c WHERE c.mountain_id = m.id) DESC,
                    (SELECT COUNT(*) FROM transportations t WHERE t.mountain_id = m.id) DESC,
                    (SELECT COUNT(*) FROM amenities a WHERE a.mountain_id = m.id) DESC,
                    (SELECT COUNT(*) FROM restaurant_sections rs WHERE rs.mountain_id = m.id) DESC,
                    (SELECT COUNT(*) FROM reviews r WHERE r.mountain_id = m.id) DESC,
                    m.id ASC
                LIMIT 1
                """, Long.class);
    }

    private Long createTemporaryUser() {
        String oauthId = "query-improvement-report-temp-user-" + System.nanoTime();
        return jdbcTemplate.queryForObject("""
                INSERT INTO users (
                    created_at, updated_at, device_type, onboarding_status, oauth_id, oauth_provider, is_deleted
                )
                VALUES (now(), now(), 'IOS', 'INCOMPLETE', ?, 'TEST', false)
                RETURNING id
                """, Long.class, oauthId);
    }

    private void printImprovementReport(
            Long mountainId,
            List<Long> statementCounts,
            List<Long> hibernateStatementCounts,
            List<Long> elapsedMs,
            MountainDetailResponse response
    ) {
        double avgElapsedMs = average(elapsedMs);
        long minElapsedMs = elapsedMs.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxElapsedMs = elapsedMs.stream().mapToLong(Long::longValue).max().orElse(0);
        int courseCount = response.courses().size();
        int transportPublicDirectionCount = response.transportations().publicTransport().size();
        int parkingDirectionCount = response.transportations().parking().size();
        int amenityDirectionCount = response.amenities().size();
        int restaurantSectionCount = response.restaurantSections().size();
        int reviewCount = response.reviews().size();

        log.info("QUERY_IMPROVEMENT mountainId={}", mountainId);
        log.info("QUERY_IMPROVEMENT statementCounts={}", statementCounts);
        log.info("QUERY_IMPROVEMENT hibernateStatementCounts={}", hibernateStatementCounts);
        log.info("QUERY_IMPROVEMENT jdbcTemplateStatementCounts=[1 per call]");
        log.info("QUERY_IMPROVEMENT elapsedMs={}", elapsedMs);
        log.info("QUERY_IMPROVEMENT avgElapsedMs={}", avgElapsedMs);
        log.info("QUERY_IMPROVEMENT minElapsedMs={}", minElapsedMs);
        log.info("QUERY_IMPROVEMENT maxElapsedMs={}", maxElapsedMs);
        log.info("QUERY_IMPROVEMENT sections courses={}"
                + " transportPublicDirections=" + transportPublicDirectionCount
                + " parkingDirections=" + parkingDirectionCount
                + " amenityDirections=" + amenityDirectionCount
                + " restaurantSections=" + restaurantSectionCount
                + " reviews=" + reviewCount, courseCount);
        log.info("QUERY_IMPROVEMENT summary mountainId={} statements={} hibernateStatements={} jdbcTemplateStatements=1"
                        + " avgMs={} minMs={} maxMs={} courses={} transportPublicDirections={}"
                        + " parkingDirections={} amenityDirections={} restaurantSections={} reviews={}",
                mountainId,
                statementCounts.stream().mapToLong(Long::longValue).max().orElse(0),
                hibernateStatementCounts.stream().mapToLong(Long::longValue).max().orElse(0),
                avgElapsedMs,
                minElapsedMs,
                maxElapsedMs,
                courseCount,
                transportPublicDirectionCount,
                parkingDirectionCount,
                amenityDirectionCount,
                restaurantSectionCount,
                reviewCount);
        writeSummaryReport(
                mountainId,
                statementCounts.stream().mapToLong(Long::longValue).max().orElse(0),
                avgElapsedMs,
                minElapsedMs,
                maxElapsedMs,
                courseCount,
                transportPublicDirectionCount,
                parkingDirectionCount,
                amenityDirectionCount,
                restaurantSectionCount,
                reviewCount
        );
    }

    private void writeSummaryReport(
            Long mountainId,
            long statements,
            double avgElapsedMs,
            long minElapsedMs,
            long maxElapsedMs,
            int courseCount,
            int transportPublicDirectionCount,
            int parkingDirectionCount,
            int amenityDirectionCount,
            int restaurantSectionCount,
            int reviewCount
    ) {
        String markdown = String.format(Locale.US, """
                # Mountain Detail Query Improvement

                Before values are fixed measurements from the pre-optimization path.
                After values are measured by this test against the current optimized path.

                | Metric | Before | After | Improvement |
                | --- | ---: | ---: | ---: |
                | SQL statements | %d | %d | %.1f%% fewer |
                | Average elapsed time | %.1fms | %.1fms | %.1f%% faster |
                | Min elapsed time | %dms | %dms | %.1f%% faster |
                | Max elapsed time | %dms | %dms | %.1f%% faster |

                | Data scope | Count |
                | --- | ---: |
                | mountainId | %d |
                | courses | %d |
                | transportPublicDirections | %d |
                | parkingDirections | %d |
                | amenityDirections | %d |
                | restaurantSections | %d |
                | reviews | %d |
                """,
                BEFORE_STATEMENTS,
                statements,
                reductionRate(BEFORE_STATEMENTS, statements),
                BEFORE_AVG_MS,
                avgElapsedMs,
                reductionRate(BEFORE_AVG_MS, avgElapsedMs),
                BEFORE_MIN_MS,
                minElapsedMs,
                reductionRate(BEFORE_MIN_MS, minElapsedMs),
                BEFORE_MAX_MS,
                maxElapsedMs,
                reductionRate(BEFORE_MAX_MS, maxElapsedMs),
                mountainId,
                courseCount,
                transportPublicDirectionCount,
                parkingDirectionCount,
                amenityDirectionCount,
                restaurantSectionCount,
                reviewCount
        );

        Path reportPath = Path.of("build/reports/query-improvement/mountain-detail-query-improvement.md");
        try {
            Files.createDirectories(reportPath.getParent());
            Files.writeString(reportPath, markdown);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        log.info("QUERY_IMPROVEMENT report={}", reportPath.toAbsolutePath());
    }

    private double average(List<Long> values) {
        return values.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
    }

    private double reductionRate(double before, double after) {
        if (before == 0) {
            return 0;
        }
        return ((before - after) / before) * 100;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.parseLong(value);
    }
}
