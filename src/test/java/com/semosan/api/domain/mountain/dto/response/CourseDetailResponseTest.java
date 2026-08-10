package com.semosan.api.domain.mountain.dto.response;

import com.semosan.api.domain.mountain.enums.Difficulty;
import com.semosan.api.domain.mountain.enums.SlopeGrade;
import com.semosan.api.domain.mountain.repository.projection.CourseDetailProjection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseDetailResponseTest {

    @Test
    void ofConvertsProjectionAndDifficulty() {
        List<SlopeSegmentResponse> segments = List.of(new SlopeSegmentResponse(0, 1, SlopeGrade.FLAT));

        CourseDetailResponse response = CourseDetailResponse.of(projection("NORMAL"), true, segments);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.mountainId()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("정상 코스");
        assertThat(response.difficulty()).isEqualTo(Difficulty.NORMAL);
        assertThat(response.distance()).isEqualTo(1500.0);
        assertThat(response.duration()).isEqualTo(90);
        assertThat(response.startName()).isEqualTo("입구");
        assertThat(response.endName()).isEqualTo("정상");
        assertThat(response.ascent()).isEqualTo(300.0);
        assertThat(response.descent()).isEqualTo(100.0);
        assertThat(response.maxAltitude()).isEqualTo(632.0);
        assertThat(response.likedByMe()).isTrue();
        assertThat(response.polyline()).isEqualTo("{\"type\":\"LineString\"}");
        assertThat(response.altitudes()).isEqualTo("[100,200]");
        assertThat(response.segments()).isSameAs(segments);
    }

    @Test
    void ofKeepsDifficultyNullWhenProjectionDifficultyIsNull() {
        CourseDetailResponse response = CourseDetailResponse.of(projection(null), false, List.of());

        assertThat(response.difficulty()).isNull();
        assertThat(response.likedByMe()).isFalse();
        assertThat(response.segments()).isEmpty();
    }

    private CourseDetailProjection projection(String difficulty) {
        CourseDetailProjection projection = mock(CourseDetailProjection.class);
        when(projection.getId()).thenReturn(1L);
        when(projection.getMountainId()).thenReturn(10L);
        when(projection.getName()).thenReturn("정상 코스");
        when(projection.getDifficulty()).thenReturn(difficulty);
        when(projection.getDistance()).thenReturn(1500.0);
        when(projection.getDuration()).thenReturn(90);
        when(projection.getStartName()).thenReturn("입구");
        when(projection.getEndName()).thenReturn("정상");
        when(projection.getAscent()).thenReturn(300.0);
        when(projection.getDescent()).thenReturn(100.0);
        when(projection.getMaxAltitude()).thenReturn(632.0);
        when(projection.getPolyline()).thenReturn("{\"type\":\"LineString\"}");
        when(projection.getAltitudes()).thenReturn("[100,200]");
        return projection;
    }
}
