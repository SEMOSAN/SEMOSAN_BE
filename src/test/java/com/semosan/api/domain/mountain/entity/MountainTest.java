package com.semosan.api.domain.mountain.entity;

import com.semosan.api.domain.mountain.enums.Difficulty;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MountainTest {

    @Test
    void updateInfoChangesBasicFields() throws Exception {
        Mountain mountain = mountain();

        mountain.updateInfo("북한산", "서울 강북구", 836.5, Difficulty.HARD, 180);

        assertThat(mountain.getName()).isEqualTo("북한산");
        assertThat(mountain.getAddress()).isEqualTo("서울 강북구");
        assertThat(mountain.getAltitude()).isEqualTo(836.5);
        assertThat(mountain.getDifficulty()).isEqualTo(Difficulty.HARD);
        assertThat(mountain.getDuration()).isEqualTo(180);
    }

    @Test
    void updateImageUrlsReplacesImages() throws Exception {
        Mountain mountain = mountain();

        mountain.updateImageUrls(List.of("image-1.jpg", "image-2.jpg"));

        assertThat(mountain.getImageUrls()).containsExactly("image-1.jpg", "image-2.jpg");
    }

    @Test
    void updateVisibilityChangesPublicFlag() throws Exception {
        Mountain mountain = mountain();

        mountain.updateVisibility(false);

        assertThat(mountain.isPublic()).isFalse();
    }

    @Test
    void updateCoordinatesUpdatesLatitudeLongitudeAndPostgisPoint() throws Exception {
        Mountain mountain = mountain();

        mountain.updateCoordinates(37.5, 127.0);

        assertThat(mountain.getLatitude()).isEqualTo(37.5);
        assertThat(mountain.getLongitude()).isEqualTo(127.0);
        assertThat(mountain.getLocation().getSRID()).isEqualTo(4326);
        assertThat(mountain.getLocation().getX()).isEqualTo(127.0);
        assertThat(mountain.getLocation().getY()).isEqualTo(37.5);
    }

    @Test
    void updateSummitUpdatesCoordinatesAndAltitude() throws Exception {
        Mountain mountain = mountain();
        ReflectionTestUtils.setField(mountain, "altitude", 632.2);

        mountain.updateSummit(37.5, 127.0, 836.5);

        assertThat(mountain.getLatitude()).isEqualTo(37.5);
        assertThat(mountain.getLongitude()).isEqualTo(127.0);
        assertThat(mountain.getAltitude()).isEqualTo(836.5);
        assertThat(mountain.getLocation().getX()).isEqualTo(127.0);
        assertThat(mountain.getLocation().getY()).isEqualTo(37.5);
    }

    @Test
    void updateSummitKeepsExistingAltitudeWhenAltitudeIsNull() throws Exception {
        Mountain mountain = mountain();
        ReflectionTestUtils.setField(mountain, "altitude", 632.2);

        mountain.updateSummit(37.5, 127.0, null);

        assertThat(mountain.getLatitude()).isEqualTo(37.5);
        assertThat(mountain.getLongitude()).isEqualTo(127.0);
        assertThat(mountain.getAltitude()).isEqualTo(632.2);
    }

    private Mountain mountain() throws Exception {
        Constructor<Mountain> constructor = Mountain.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Mountain mountain = constructor.newInstance();
        ReflectionTestUtils.setField(mountain, "id", 1L);
        return mountain;
    }
}
