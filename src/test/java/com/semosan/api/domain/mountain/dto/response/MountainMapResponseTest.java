package com.semosan.api.domain.mountain.dto.response;

import com.semosan.api.domain.mountain.repository.projection.MountainMapProjection;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MountainMapResponseTest {

    @Test
    void fromMarksVisitedWhenVisitCountIsPositive() {
        MountainMapProjection projection = projection(3L);

        MountainMapResponse response = MountainMapResponse.from(projection);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("관악산");
        assertThat(response.latitude()).isEqualTo(37.5);
        assertThat(response.longitude()).isEqualTo(127.0);
        assertThat(response.visited()).isTrue();
        assertThat(response.visitCount()).isEqualTo(3L);
        assertThat(response.imageUrl()).isEqualTo("image.jpg");
    }

    @Test
    void fromUsesZeroVisitCountWhenProjectionVisitCountIsNull() {
        MountainMapProjection projection = projection(null);

        MountainMapResponse response = MountainMapResponse.from(projection);

        assertThat(response.visited()).isFalse();
        assertThat(response.visitCount()).isZero();
    }

    @Test
    void fromMarksNotVisitedWhenVisitCountIsZero() {
        MountainMapProjection projection = projection(0L);

        MountainMapResponse response = MountainMapResponse.from(projection);

        assertThat(response.visited()).isFalse();
        assertThat(response.visitCount()).isZero();
    }

    private MountainMapProjection projection(Long visitCount) {
        MountainMapProjection projection = mock(MountainMapProjection.class);
        when(projection.getId()).thenReturn(1L);
        when(projection.getName()).thenReturn("관악산");
        when(projection.getLatitude()).thenReturn(37.5);
        when(projection.getLongitude()).thenReturn(127.0);
        when(projection.getVisitCount()).thenReturn(visitCount);
        when(projection.getImageUrl()).thenReturn("image.jpg");
        return projection;
    }
}
