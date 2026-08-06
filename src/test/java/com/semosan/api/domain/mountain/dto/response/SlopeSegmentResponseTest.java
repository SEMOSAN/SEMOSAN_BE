package com.semosan.api.domain.mountain.dto.response;

import com.semosan.api.domain.mountain.enums.SlopeGrade;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlopeSegmentResponseTest {

    @Test
    void recordKeepsStartEndAndGrade() {
        SlopeSegmentResponse response = new SlopeSegmentResponse(1, 3, SlopeGrade.MILD_UP);

        assertThat(response.startIdx()).isEqualTo(1);
        assertThat(response.endIdx()).isEqualTo(3);
        assertThat(response.grade()).isEqualTo(SlopeGrade.MILD_UP);
    }
}
