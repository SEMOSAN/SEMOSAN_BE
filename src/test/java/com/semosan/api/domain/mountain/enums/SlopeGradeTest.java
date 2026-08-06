package com.semosan.api.domain.mountain.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlopeGradeTest {

    @Test
    void classifyReturnsSteepDownAtAndBelowNegativeSteepThreshold() {
        assertThat(SlopeGrade.classify(-15.0)).isEqualTo(SlopeGrade.STEEP_DOWN);
        assertThat(SlopeGrade.classify(-16.0)).isEqualTo(SlopeGrade.STEEP_DOWN);
    }

    @Test
    void classifyReturnsMildDownAtAndBelowNegativeMildThreshold() {
        assertThat(SlopeGrade.classify(-5.0)).isEqualTo(SlopeGrade.MILD_DOWN);
        assertThat(SlopeGrade.classify(-14.999)).isEqualTo(SlopeGrade.MILD_DOWN);
    }

    @Test
    void classifyReturnsFlatBetweenMildThresholds() {
        assertThat(SlopeGrade.classify(-4.999)).isEqualTo(SlopeGrade.FLAT);
        assertThat(SlopeGrade.classify(0.0)).isEqualTo(SlopeGrade.FLAT);
        assertThat(SlopeGrade.classify(4.999)).isEqualTo(SlopeGrade.FLAT);
    }

    @Test
    void classifyReturnsMildUpAtAndBelowPositiveSteepThreshold() {
        assertThat(SlopeGrade.classify(5.0)).isEqualTo(SlopeGrade.MILD_UP);
        assertThat(SlopeGrade.classify(14.999)).isEqualTo(SlopeGrade.MILD_UP);
    }

    @Test
    void classifyReturnsSteepUpAtAndAbovePositiveSteepThreshold() {
        assertThat(SlopeGrade.classify(15.0)).isEqualTo(SlopeGrade.STEEP_UP);
        assertThat(SlopeGrade.classify(16.0)).isEqualTo(SlopeGrade.STEEP_UP);
    }
}
