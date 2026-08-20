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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "BASELINE", matches = "true")
class MountainDetailQueryBaselineTest {

    private static final Logger log = LoggerFactory.getLogger(MountainDetailQueryBaselineTest.class);

    @MockitoBean
    private FirebaseConfig firebaseConfig;

    @Autowired
    private MountainService mountainService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void measureMountainDetailQueryBaseline() {
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
            printBaseline(mountainId, statementCounts, hibernateStatementCounts, elapsedMs, lastResponse);
        } finally {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        }
    }

    private Long resolveMountainId() {
        Long requestedMountainId = parseLong(System.getenv("BASELINE_MOUNTAIN_ID"));
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
        String oauthId = "baseline-temp-user-" + System.nanoTime();
        return jdbcTemplate.queryForObject("""
                INSERT INTO users (
                    created_at, updated_at, device_type, onboarding_status, oauth_id, oauth_provider, is_deleted
                )
                VALUES (now(), now(), 'IOS', 'INCOMPLETE', ?, 'TEST', false)
                RETURNING id
                """, Long.class, oauthId);
    }

    private void printBaseline(
            Long mountainId,
            List<Long> statementCounts,
            List<Long> hibernateStatementCounts,
            List<Long> elapsedMs,
            MountainDetailResponse response
    ) {
        log.info("BASELINE mountainId={}", mountainId);
        log.info("BASELINE statementCounts={}", statementCounts);
        log.info("BASELINE hibernateStatementCounts={}", hibernateStatementCounts);
        log.info("BASELINE jdbcTemplateStatementCounts=[1 per call]");
        log.info("BASELINE elapsedMs={}", elapsedMs);
        log.info("BASELINE avgElapsedMs={}", average(elapsedMs));
        log.info("BASELINE minElapsedMs={}", elapsedMs.stream().mapToLong(Long::longValue).min().orElse(0));
        log.info("BASELINE maxElapsedMs={}", elapsedMs.stream().mapToLong(Long::longValue).max().orElse(0));
        log.info("BASELINE sections courses={}"
                + " transportPublicDirections=" + response.transportations().publicTransport().size()
                + " parkingDirections=" + response.transportations().parking().size()
                + " amenityDirections=" + response.amenities().size()
                + " restaurantSections=" + response.restaurantSections().size()
                + " reviews=" + response.reviews().size(), response.courses().size());
    }

    private double average(List<Long> values) {
        return values.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.parseLong(value);
    }
}
