package com.semosan.api.domain.mountain.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FitnessLevelTest {

    @Test
    void getLabelReturnsKoreanLabel() {
        assertThat(FitnessLevel.ENTRY.getLabel()).isEqualTo("입문");
        assertThat(FitnessLevel.BEGINNER.getLabel()).isEqualTo("초");
        assertThat(FitnessLevel.INTERMEDIATE.getLabel()).isEqualTo("중");
        assertThat(FitnessLevel.ADVANCED.getLabel()).isEqualTo("상");
    }
}
