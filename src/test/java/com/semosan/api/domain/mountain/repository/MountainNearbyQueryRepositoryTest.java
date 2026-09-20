package com.semosan.api.domain.mountain.repository;

import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.repository.projection.NearbyMountainProjection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 PostGIS에서 반경 조건과 JPA projection 매핑을 함께 검증한다. 데이터는 테스트마다 롤백한다. */
@SpringBootTest(classes = MountainNearbyQueryRepositoryTest.TestConfig.class)
@ActiveProfiles("test")
@Transactional
class MountainNearbyQueryRepositoryTest {

    // 국내 시드 데이터와 겹치지 않는 기준점. WGS84 구면 반지름으로 경계용 좌표를 만든다.
    private static final double LAT = -30.0;
    private static final double LNG = -120.0;
    private static final double SPHERE_RADIUS = (2 * 6_378_137.0 + 6_356_752.314245179) / 3;

    @Autowired
    private MountainRepository mountainRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void includesSelectedMountainAndBoundaryButExcludesOutsideRadius() {
        Long center = insertMountainAtDistance("현재 산", 0, true);
        Long inside = insertMountainAtDistance("경계 안", 1_999.99, true);
        Long boundary = insertMountainAtDistance("2km 경계", 2_000, true);
        insertMountainAtDistance("경계 밖", 2_000.01, true);

        List<NearbyMountainProjection> result = findNearby();

        assertThat(result).extracting(NearbyMountainProjection::getMountainId)
                .containsExactly(center, inside, boundary);
        assertThat(result).extracting(NearbyMountainProjection::getName)
                .containsExactly("현재 산", "경계 안", "2km 경계");
        assertThat(mountainRepository.findNearestByLatLng(LAT, LNG).orElseThrow().getId()).isEqualTo(center);
    }

    @Test
    void excludesPrivateAndMissingCoordinatesButIncludesMountainWithoutCourses() {
        Long visible = insertMountainAtDistance("공개 산", 500, true);
        insertMountainAtDistance("비공개 산", 0, false);
        insertMountain("위도 없음", null, LNG, true);
        insertMountain("경도 없음", LAT, null, true);
        insertMountain("좌표 없음", null, null, true);

        assertThat(findNearby()).extracting(NearbyMountainProjection::getMountainId).containsExactly(visible);
        assertThat(courseRepository.findByMountainIdOrderByIdAsc(visible)).isEmpty();
        assertThat(mountainRepository.findNearestByLatLng(LAT, LNG).orElseThrow().getId()).isEqualTo(visible);
    }

    @Test
    void sortsByDistanceThenIdConsistentlyWithDefaultMountain() {
        Long far = insertMountainAtDistance("먼 산", 1_500, true);
        Long nearFirst = insertMountainAtDistance("가까운 산 1", 300, true);
        Long nearSecond = insertMountainAtDistance("가까운 산 2", 300, true);

        assertThat(findNearby()).extracting(NearbyMountainProjection::getMountainId)
                .containsExactly(nearFirst, nearSecond, far);
        assertThat(mountainRepository.findNearestByLatLng(LAT, LNG).orElseThrow().getId()).isEqualTo(nearFirst);
    }

    @Test
    void keepsNearestMountainAvailableWhenNoneWithinRadius() {
        Long outside = insertMountainAtDistance("반경 밖 기본 산", 3_000, true);

        assertThat(findNearby()).isEmpty();
        assertThat(mountainRepository.findNearestByLatLng(LAT, LNG).orElseThrow().getId()).isEqualTo(outside);
    }

    @Test
    void returnsCoursesInIdOrderRegardlessOfInsertionOrder() {
        Long mountainId = insertMountainAtDistance("코스 정렬 산", 0, true);
        Long firstId = nextCourseId();
        Long secondId = nextCourseId();
        insertCourse(secondId, mountainId, "두 번째 코스");
        insertCourse(firstId, mountainId, "첫 번째 코스");

        assertThat(courseRepository.findByMountainIdOrderByIdAsc(mountainId))
                .extracting(Course::getId).containsExactly(firstId, secondId);
    }

    private List<NearbyMountainProjection> findNearby() {
        return mountainRepository.findNearbyByLatLng(LAT, LNG, 2_000);
    }

    private Long insertMountainAtDistance(String name, double meters, boolean isPublic) {
        return insertMountain(name, LAT + Math.toDegrees(meters / SPHERE_RADIUS), LNG, isPublic);
    }

    private Long insertMountain(String name, Double latitude, Double longitude, boolean isPublic) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO mountains (
                    created_at, updated_at, name, address, altitude, difficulty,
                    latitude, longitude, is_public
                )
                VALUES (now(), now(), ?, '공간 쿼리 테스트', 500, 'NORMAL', ?, ?, ?)
                RETURNING id
                """, Long.class, name, latitude, longitude, isPublic);
    }

    private Long nextCourseId() {
        return jdbcTemplate.queryForObject("SELECT nextval(pg_get_serial_sequence('courses', 'id'))", Long.class);
    }

    private void insertCourse(Long id, Long mountainId, String name) {
        jdbcTemplate.update("""
                INSERT INTO courses (id, created_at, updated_at, mountain_id, name, difficulty, distance, duration)
                VALUES (?, now(), now(), ?, ?, 'NORMAL', 1_000, 30)
                """, id, mountainId, name);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("com.semosan.api.domain")
    @EnableJpaRepositories(basePackageClasses = MountainRepository.class)
    static class TestConfig {
    }
}
