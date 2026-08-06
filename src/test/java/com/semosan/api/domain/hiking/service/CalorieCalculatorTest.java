package com.semosan.api.domain.hiking.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalorieCalculatorTest {

    @Test
    void calculateReturnsZeroWhenDurationIsNotPositive() {
        assertThat(CalorieCalculator.calculate(70.0, 1000.0, 100.0, 0)).isZero();
        assertThat(CalorieCalculator.calculate(70.0, 1000.0, 100.0, -1)).isZero();
    }

    @Test
    void calculateUsesDefaultWeightWhenWeightMissingOrInvalid() {
        assertThat(CalorieCalculator.calculate(null, 1000.0, 0.0, 3600)).isEqualTo(390);
        assertThat(CalorieCalculator.calculate(0.0, 1000.0, 0.0, 3600)).isEqualTo(390);
    }

    @Test
    void calculateUsesEasyMetWhenGradeDataIsMissingOrLow() {
        assertThat(CalorieCalculator.calculate(70.0, null, 100.0, 3600)).isEqualTo(420);
        assertThat(CalorieCalculator.calculate(70.0, 1000.0, 40.0, 3600)).isEqualTo(420);
    }

    @Test
    void calculateUsesMediumMetWhenGradeIsModerate() {
        assertThat(CalorieCalculator.calculate(70.0, 1000.0, 70.0, 3600)).isEqualTo(525);
    }

    @Test
    void calculateUsesHardMetWhenGradeIsSteep() {
        assertThat(CalorieCalculator.calculate(70.0, 1000.0, 100.0, 3600)).isEqualTo(630);
    }
}
