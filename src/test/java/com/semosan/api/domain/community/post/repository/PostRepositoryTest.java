package com.semosan.api.domain.community.post.repository;

import com.semosan.api.domain.community.post.entity.FreePost;
import com.semosan.api.domain.community.post.entity.Post;
import com.semosan.api.domain.community.post.entity.RecordPost;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void increaseViewCountIncrementsFreePostViewCountInDatabase() {
        Long userId = insertUser();
        Long postId = insertPost(userId, "FREE", 4);
        insertFreePost(postId);

        Post loaded = postRepository.findById(postId).orElseThrow();
        assertThat(loaded).isInstanceOf(FreePost.class);

        postRepository.increaseViewCount(postId);
        entityManager.flush();
        entityManager.clear();

        Post result = postRepository.findById(postId).orElseThrow();
        assertThat(result.getViewCount()).isEqualTo(5);
    }

    @Test
    void increaseViewCountIncrementsRecordPostViewCountInDatabase() {
        Long userId = insertUser();
        Long mountainId = insertMountain();
        Long hikingRecordId = insertHikingRecord(mountainId);
        Long postId = insertPost(userId, "RECORD", 7);
        insertRecordPost(postId, hikingRecordId);

        Post loaded = postRepository.findById(postId).orElseThrow();
        assertThat(loaded).isInstanceOf(RecordPost.class);

        postRepository.increaseViewCount(postId);
        entityManager.flush();
        entityManager.clear();

        Post result = postRepository.findById(postId).orElseThrow();
        assertThat(result.getViewCount()).isEqualTo(8);
    }

    private Long insertUser() {
        String oauthId = "post-repository-test-" + System.nanoTime();
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
                    now(), now(), '테스트산', '서울 테스트구', 123.4, 'NORMAL', 90,
                    '[]'::jsonb, 37.5, 127.0, true
                )
                RETURNING id
                """, Long.class);
    }

    private Long insertHikingRecord(Long mountainId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO hiking_records (
                    created_at, updated_at, mountain_id, duration, max_altitude, calories,
                    paused_seconds_total
                )
                VALUES (now(), now(), ?, 3600, 123.4, 300, 0)
                RETURNING id
                """, Long.class, mountainId);
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

    private void insertRecordPost(Long postId, Long hikingRecordId) {
        jdbcTemplate.update("""
                INSERT INTO record_posts (id, hiking_record_id)
                VALUES (?, ?)
                """, postId, hikingRecordId);
    }
}
