package com.semosan.api.domain.mountain.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse;
import com.semosan.api.domain.mountain.enums.AmenityType;
import com.semosan.api.domain.mountain.enums.Difficulty;
import com.semosan.api.domain.mountain.enums.TransportationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = MountainDetailQueryRepositoryTest.TestConfig.class)
@ActiveProfiles("test")
@Transactional
class MountainDetailQueryRepositoryTest {

    @Autowired
    private MountainDetailQueryRepository mountainDetailQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findDetailByMountainIdReturnsAggregatedMountainDetail() {
        Long mountainId = insertMountain();
        Long courseId = insertCourse(mountainId);
        insertTransportation(mountainId, TransportationType.SUBWAY, "북문", "1호선", "역에서 도보 10분");
        insertTransportation(mountainId, TransportationType.PARKING, "남문", "공영주차장", "주차 가능");
        insertAmenity(mountainId, AmenityType.RESTROOM, "북문");
        insertAmenity(mountainId, AmenityType.STORE, "북문");
        Long sectionId = insertRestaurantSection(mountainId);
        insertRestaurant(sectionId);
        Long userId = insertUser();
        insertReview(mountainId, courseId, userId);

        Optional<MountainDetailResponse> result = mountainDetailQueryRepository.findDetailByMountainId(mountainId);

        assertThat(result).isPresent();
        MountainDetailResponse response = result.get();
        assertThat(response.mountain().mountainId()).isEqualTo(mountainId);
        assertThat(response.mountain().name()).isEqualTo("테스트산");
        assertThat(response.mountain().address()).isEqualTo("서울 테스트구");
        assertThat(response.mountain().altitude()).isEqualTo(123.4);
        assertThat(response.mountain().difficulty()).isEqualTo(Difficulty.NORMAL);
        assertThat(response.mountain().duration()).isEqualTo(90);
        assertThat(response.mountain().imageUrls()).containsExactly("https://example.com/1.jpg", "https://example.com/2.jpg");
        assertThat(response.mountain().latitude()).isEqualTo(37.5);
        assertThat(response.mountain().longitude()).isEqualTo(127.0);

        assertThat(response.courses()).hasSize(1);
        assertThat(response.courses().getFirst().courseId()).isEqualTo(courseId);
        assertThat(response.courses().getFirst().name()).isEqualTo("능선 코스");
        assertThat(response.courses().getFirst().difficulty()).isEqualTo(Difficulty.EASY);
        assertThat(response.courses().getFirst().distance()).isEqualTo(3500.0);
        assertThat(response.courses().getFirst().duration()).isEqualTo(70);
        assertThat(response.courses().getFirst().startName()).isEqualTo("입구");
        assertThat(response.courses().getFirst().endName()).isEqualTo("정상");

        assertThat(response.transportations().publicTransport()).containsOnlyKeys("북문");
        assertThat(response.transportations().publicTransport().get("북문")).hasSize(1);
        assertThat(response.transportations().publicTransport().get("북문").getFirst().type())
                .isEqualTo(TransportationType.SUBWAY);
        assertThat(response.transportations().parking()).containsOnlyKeys("남문");
        assertThat(response.transportations().parking().get("남문").getFirst().name()).isEqualTo("공영주차장");

        assertThat(response.amenities()).containsOnlyKeys("북문");
        assertThat(response.amenities().get("북문")).containsExactly(AmenityType.RESTROOM, AmenityType.STORE);

        assertThat(response.restaurantSections()).hasSize(1);
        assertThat(response.restaurantSections().getFirst().title()).isEqualTo("등산 후 식당");
        assertThat(response.restaurantSections().getFirst().restaurants()).hasSize(1);
        assertThat(response.restaurantSections().getFirst().restaurants().getFirst().name()).isEqualTo("테스트식당");
        assertThat(response.restaurantSections().getFirst().restaurants().getFirst().mapUrl())
                .isEqualTo("https://map.example.com/restaurant");

        assertThat(response.reviews()).hasSize(1);
        assertThat(response.reviews().getFirst().authorName()).isEqualTo("테스터");
        assertThat(response.reviews().getFirst().content()).isEqualTo("좋은 코스입니다");
        assertThat(response.reviews().getFirst().difficulty()).isEqualTo(Difficulty.HARD);
        assertThat(response.reviews().getFirst().courseName()).isEqualTo("능선 코스");
    }

    @Test
    void findDetailByMountainIdReturnsEmptyWhenMountainDoesNotExist() {
        Optional<MountainDetailResponse> result = mountainDetailQueryRepository.findDetailByMountainId(-1L);

        assertThat(result).isEmpty();
    }

    private Long insertMountain() {
        return jdbcTemplate.queryForObject("""
                INSERT INTO mountains (
                    created_at, updated_at, name, address, altitude, difficulty, duration,
                    image_urls, latitude, longitude, is_public
                )
                VALUES (
                    now(), now(), '테스트산', '서울 테스트구', 123.4, 'NORMAL', 90,
                    '["https://example.com/1.jpg", "https://example.com/2.jpg"]'::jsonb,
                    37.5, 127.0, true
                )
                RETURNING id
                """, Long.class);
    }

    private Long insertCourse(Long mountainId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO courses (
                    created_at, updated_at, mountain_id, name, difficulty, distance, duration,
                    start_name, end_name
                )
                VALUES (now(), now(), ?, '능선 코스', 'EASY', 3500.0, 70, '입구', '정상')
                RETURNING id
                """, Long.class, mountainId);
    }

    private void insertTransportation(
            Long mountainId,
            TransportationType type,
            String direction,
            String name,
            String description
    ) {
        jdbcTemplate.update("""
                INSERT INTO transportations (
                    created_at, updated_at, mountain_id, type, direction, name, description
                )
                VALUES (now(), now(), ?, ?, ?, ?, ?)
                """, mountainId, type.name(), direction, name, description);
    }

    private void insertAmenity(Long mountainId, AmenityType type, String direction) {
        jdbcTemplate.update("""
                INSERT INTO amenities (created_at, updated_at, mountain_id, type, direction)
                VALUES (now(), now(), ?, ?, ?)
                """, mountainId, type.name(), direction);
    }

    private Long insertRestaurantSection(Long mountainId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO restaurant_sections (created_at, updated_at, mountain_id, title)
                VALUES (now(), now(), ?, '등산 후 식당')
                RETURNING id
                """, Long.class, mountainId);
    }

    private void insertRestaurant(Long sectionId) {
        jdbcTemplate.update("""
                INSERT INTO restaurants (
                    created_at, updated_at, section_id, name, category, image_url, map_url
                )
                VALUES (
                    now(), now(), ?, '테스트식당', '한식',
                    'https://example.com/restaurant.jpg', 'https://map.example.com/restaurant'
                )
                """, sectionId);
    }

    private Long insertUser() {
        String oauthId = "mountain-detail-query-repository-test-" + System.nanoTime();
        return jdbcTemplate.queryForObject("""
                INSERT INTO users (
                    created_at, updated_at, device_type, onboarding_status, oauth_id, oauth_provider,
                    is_deleted, name
                )
                VALUES (now(), now(), 'IOS', 'INCOMPLETE', ?, 'TEST', false, '테스터')
                RETURNING id
                """, Long.class, oauthId);
    }

    private void insertReview(Long mountainId, Long courseId, Long userId) {
        jdbcTemplate.update("""
                INSERT INTO reviews (
                    created_at, updated_at, mountain_id, course_id, user_id,
                    content, difficulty, image_url
                )
                VALUES (
                    now(), now(), ?, ?, ?, '좋은 코스입니다', 'HARD',
                    'https://example.com/review.jpg'
                )
                """, mountainId, courseId, userId);
    }

    @EnableAutoConfiguration(exclude = {
            HibernateJpaAutoConfiguration.class,
            DataJpaRepositoriesAutoConfiguration.class
    })
    @Import(MountainDetailQueryRepository.class)
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
