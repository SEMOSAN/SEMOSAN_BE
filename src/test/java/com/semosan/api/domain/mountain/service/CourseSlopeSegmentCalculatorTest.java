package com.semosan.api.domain.mountain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semosan.api.domain.mountain.dto.response.SlopeSegmentResponse;
import com.semosan.api.domain.mountain.enums.SlopeGrade;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CourseSlopeSegmentCalculatorTest {

    private final CourseSlopeSegmentCalculator calculator = new CourseSlopeSegmentCalculator(new ObjectMapper());

    @Test
    void calculateReturnsEmptyWhenPolylineOrAltitudesAreNull() {
        assertThat(calculator.calculate(1L, null, "[1,2]")).isEmpty();
        assertThat(calculator.calculate(1L, lineString(2), null)).isEmpty();
    }

    @Test
    void calculateReturnsEmptyWhenCoordinatesAreMissingInvalidOrTooShort() {
        assertThat(calculator.calculate(1L, "{}", "[1,2]")).isEmpty();
        assertThat(calculator.calculate(1L, "{\"coordinates\":{}}", "[1,2]")).isEmpty();
        assertThat(calculator.calculate(1L, lineString(1), "[1]")).isEmpty();
    }

    @Test
    void calculateReturnsEmptyWhenAltitudesAreNullOrSizeMismatch() {
        assertThat(calculator.calculate(1L, lineString(2), "null")).isEmpty();
        assertThat(calculator.calculate(1L, lineString(3), "[100,110]")).isEmpty();
    }

    @Test
    void calculateReturnsEmptyWhenJsonParsingFails() {
        assertThat(calculator.calculate(1L, "{invalid", "[100,110]")).isEmpty();
        assertThat(calculator.calculate(1L, lineString(2), "[invalid")).isEmpty();
    }

    @Test
    void calculateCreatesSingleFlatSegmentForSameLocationOrNullAltitudes() {
        String sameLocation = """
                {"coordinates":[[127.0,37.0],[127.0,37.0]]}
                """;
        String withNullAltitude = """
                {"coordinates":[[127.0,37.0],[127.001,37.0]]}
                """;

        assertThat(calculator.calculate(1L, sameLocation, "[100,200]"))
                .containsExactly(new SlopeSegmentResponse(0, 1, SlopeGrade.FLAT));
        assertThat(calculator.calculate(1L, withNullAltitude, "[100,null]"))
                .containsExactly(new SlopeSegmentResponse(0, 1, SlopeGrade.FLAT));
    }

    @Test
    void calculateGroupsContinuousSameGradesAndSplitsWhenGradeChanges() {
        String polyline = """
                {"coordinates":[
                  [127.0000,37.0000],
                  [127.0010,37.0000],
                  [127.0020,37.0000],
                  [127.0030,37.0000],
                  [127.0040,37.0000],
                  [127.0050,37.0000],
                  [127.0060,37.0000],
                  [127.0070,37.0000],
                  [127.0080,37.0000],
                  [127.0090,37.0000],
                  [127.0100,37.0000],
                  [127.0110,37.0000]
                ]}
                """;

        List<SlopeSegmentResponse> result = calculator.calculate(1L, 
                polyline,
                "[0,0,0,0,1000,1000,1000,1000,0,0,0,0]"
        );

        assertThat(result).containsExactly(
                new SlopeSegmentResponse(0, 1, SlopeGrade.FLAT),
                new SlopeSegmentResponse(1, 5, SlopeGrade.STEEP_UP),
                new SlopeSegmentResponse(5, 6, SlopeGrade.FLAT),
                new SlopeSegmentResponse(6, 10, SlopeGrade.STEEP_DOWN),
                new SlopeSegmentResponse(10, 11, SlopeGrade.FLAT)
        );
    }

    private String lineString(int pointCount) {
        StringBuilder coordinates = new StringBuilder();
        coordinates.append("{\"coordinates\":[");
        for (int i = 0; i < pointCount; i++) {
            if (i > 0) {
                coordinates.append(",");
            }
            coordinates.append("[127.").append(i).append(",37.0]");
        }
        coordinates.append("]}");
        return coordinates.toString();
    }
}
