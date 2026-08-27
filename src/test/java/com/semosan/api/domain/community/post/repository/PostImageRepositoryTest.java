package com.semosan.api.domain.community.post.repository;

import com.semosan.api.common.config.FirebaseConfig;
import com.semosan.api.domain.community.post.entity.PostImage;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostImageRepositoryTest {

    @MockitoBean
    private FirebaseConfig firebaseConfig;

    @Autowired
    private PostImageRepository postImageRepository;

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
    @DisplayName("[Before vs After] 대량 데이터(1,000건) 대상 실행 시간(ms) 및 1차 캐시 엔티티 점유량 비교")
    void benchmarkBeforeVsAfterWithLargeDataset() {
        int count = 1000;
        Long userId = insertUser();
        List<Long> postIds = new ArrayList<>(count);

        System.out.println(">> 데이터 " + count + "건 세팅 시작...");
        for (int i = 1; i <= count; i++) {
            Long postId = insertPost(userId, "FREE", 0);
            insertFreePost(postId);
            insertPostImage(postId, "https://example.com/images/semosan_main_banner_" + i + ".png", true);
            postIds.add(postId);
        }
        entityManager.flush();
        entityManager.clear();
        System.out.println(">> 데이터 세팅 완료");

        // ----------------------------------------------------
        // 1. Before 방식 측정 (엔티티 조회 + 7개 컬럼 + 1차 캐시 적재)
        // ----------------------------------------------------
        entityManager.clear();
        statistics.clear();

        long beforeStartTime = System.nanoTime();
        List<PostImage> beforeEntities = entityManager.createQuery(
                "SELECT pi FROM PostImage pi WHERE pi.post.id IN :postIds AND pi.main = true",
                PostImage.class
        ).setParameter("postIds", postIds).getResultList();

        Map<Long, String> beforeMap = beforeEntities.stream()
                .collect(Collectors.toMap(img -> img.getPost().getId(), PostImage::getImageUrl));

        long beforeEndTime = System.nanoTime();
        double beforeDurationMs = (beforeEndTime - beforeStartTime) / 1_000_000.0;
        long beforeEntityLoadCount = statistics.getEntityLoadCount();

        // ----------------------------------------------------
        // 2. After 방식 측정 (PR #350: 2개 컬럼 Projection + 1차 캐시 미적재)
        // ----------------------------------------------------
        entityManager.clear();
        statistics.clear();

        long afterStartTime = System.nanoTime();
        List<Object[]> afterRows = postImageRepository.findMainImagesByPostIds(postIds);

        Map<Long, String> afterMap = afterRows.stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (String) row[1]));

        long afterEndTime = System.nanoTime();
        double afterDurationMs = (afterEndTime - afterStartTime) / 1_000_000.0;
        long afterEntityLoadCount = statistics.getEntityLoadCount();

        // ----------------------------------------------------
        // 3. 결과 출력 및 검증
        // ----------------------------------------------------
        System.out.println("\n=================================================");
        System.out.println("📊 [1,000건 조회 벤치마크 결과 비교]");
        System.out.println("-------------------------------------------------");
        System.out.printf("• Before 소요 시간: %.2f ms | 로드된 엔티티 수: %d 개\n", beforeDurationMs, beforeEntityLoadCount);
        System.out.printf("• After  소요 시간: %.2f ms | 로드된 엔티티 수: %d 개\n", afterDurationMs, afterEntityLoadCount);
        if (beforeDurationMs > 0) {
            double speedup = ((beforeDurationMs - afterDurationMs) / beforeDurationMs) * 100.0;
            System.out.printf("• 속도 개선율: %.1f%%\n", speedup);
        }
        System.out.println("=================================================\n");

        assertThat(beforeMap).hasSize(count);
        assertThat(afterMap).hasSize(count);
        assertThat(afterEntityLoadCount).isEqualTo(0); // After 방식은 엔티티 로드가 전혀 없음
        assertThat(beforeEntityLoadCount).isEqualTo(count); // Before 방식은 1,000개 엔티티가 1차 캐시에 로드됨
    }

    private Long insertUser() {
        String oauthId = "post-image-test-" + System.nanoTime();
        return jdbcTemplate.queryForObject("""
                INSERT INTO users (
                    created_at, updated_at, device_type, onboarding_status, oauth_id, oauth_provider,
                    is_deleted, name
                )
                VALUES (now(), now(), 'IOS', 'INCOMPLETE', ?, 'TEST', false, '테스터')
                RETURNING id
                """, Long.class, oauthId);
    }

    private Long insertPost(Long authorId, String postType, int viewCount) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO posts (
                    post_type, created_at, updated_at, content, is_deleted, view_count, author_id
                )
                VALUES (?, now(), now(), '본문', false, ?, ?)
                RETURNING id
                """, Long.class, postType, viewCount, authorId);
    }

    private void insertFreePost(Long postId) {
        jdbcTemplate.update("""
                INSERT INTO free_posts (id, title)
                VALUES (?, '제목')
                """, postId);
    }

    private void insertPostImage(Long postId, String imageUrl, boolean isMain) {
        jdbcTemplate.update("""
                INSERT INTO post_images (
                    created_at, updated_at, post_id, image_url, sort_order, is_main
                )
                VALUES (now(), now(), ?, ?, 0, ?)
                """, postId, imageUrl, isMain);
    }
}
