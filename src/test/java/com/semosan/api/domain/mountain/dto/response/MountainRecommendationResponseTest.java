package com.semosan.api.domain.mountain.dto.response;

import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.enums.Difficulty;
import com.semosan.api.domain.mountain.service.recommendation.TrackScorer.TrackMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MountainRecommendationResponseTest {

    @Test
    void ofUsesFirstImageAndMetricsHeight() throws Exception {
        Mountain mountain = mountain(Difficulty.NORMAL, List.of("first.jpg", "second.jpg"), 632.2);
        TrackMetrics metrics = new TrackMetrics(5.0, 300.0, 700, 0, 1, 1, 1);

        MountainRecommendationResponse response = MountainRecommendationResponse.of(mountain, metrics);

        assertThat(response.mountainId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("관악산");
        assertThat(response.imageUrl()).isEqualTo("first.jpg");
        assertThat(response.difficultyLabel()).isEqualTo("중");
        assertThat(response.mountainHeightM()).isEqualTo(700);
        assertThat(response.address()).isEqualTo("서울 관악구");
    }

    @Test
    void fromDefaultMountainUsesAltitudeAndEasyLabel() throws Exception {
        Mountain mountain = mountain(Difficulty.EASY, List.of("easy.jpg"), 123.9);

        MountainRecommendationResponse response = MountainRecommendationResponse.fromDefaultMountain(mountain);

        assertThat(response.imageUrl()).isEqualTo("easy.jpg");
        assertThat(response.difficultyLabel()).isEqualTo("초");
        assertThat(response.mountainHeightM()).isEqualTo(123);
    }

    @Test
    void ofReturnsNullImageWhenImageUrlsAreNull() throws Exception {
        Mountain mountain = mountain(Difficulty.HARD, null, 900.0);
        TrackMetrics metrics = new TrackMetrics(10.0, 600.0, 900, 1, 2, 0, 0);

        MountainRecommendationResponse response = MountainRecommendationResponse.of(mountain, metrics);

        assertThat(response.imageUrl()).isNull();
        assertThat(response.difficultyLabel()).isEqualTo("상");
    }

    @Test
    void fromDefaultMountainReturnsNullImageWhenImageUrlsAreEmpty() throws Exception {
        Mountain mountain = mountain(Difficulty.NORMAL, List.of(), 500.0);

        MountainRecommendationResponse response = MountainRecommendationResponse.fromDefaultMountain(mountain);

        assertThat(response.imageUrl()).isNull();
        assertThat(response.difficultyLabel()).isEqualTo("중");
    }

    private Mountain mountain(Difficulty difficulty, List<String> imageUrls, Double altitude) throws Exception {
        Constructor<Mountain> constructor = Mountain.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Mountain mountain = constructor.newInstance();
        ReflectionTestUtils.setField(mountain, "id", 1L);
        ReflectionTestUtils.setField(mountain, "name", "관악산");
        ReflectionTestUtils.setField(mountain, "address", "서울 관악구");
        ReflectionTestUtils.setField(mountain, "difficulty", difficulty);
        ReflectionTestUtils.setField(mountain, "imageUrls", imageUrls);
        ReflectionTestUtils.setField(mountain, "altitude", altitude);
        return mountain;
    }
}
