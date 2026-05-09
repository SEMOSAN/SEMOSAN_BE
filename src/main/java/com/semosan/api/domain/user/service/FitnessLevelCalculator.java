package com.semosan.api.domain.user.service;

import com.semosan.api.domain.user.dto.request.RegisterOnboardingRequest;
import com.semosan.api.domain.user.enums.ExerciseDuration;
import com.semosan.api.domain.user.enums.ExerciseFrequency;
import com.semosan.api.domain.user.enums.FitnessLevel;
import com.semosan.api.domain.user.enums.HikingLevel;
import org.springframework.stereotype.Component;

@Component
public class FitnessLevelCalculator {

    // 온보딩 응답을 점수화해 사용자의 체력 레벨을 계산합니다.
    public FitnessLevel calculate(RegisterOnboardingRequest request) {
        int score = hikingScore(request.hikingLevel())
                + frequencyScore(request.exerciseFrequency())
                + durationScore(request.exerciseDuration());

        if (score >= 7) {
            return FitnessLevel.HIGH;
        }
        if (score >= 4) {
            return FitnessLevel.MEDIUM;
        }
        return FitnessLevel.LOW;
    }

    // 등산 경험 수준을 체력 점수로 변환합니다.
    private int hikingScore(HikingLevel hikingLevel) {
        return switch (hikingLevel) {
            case EXPERT -> 3;
            case HOBBY, EXPERIENCED -> 2;
            case BEGINNER -> 1;
        };
    }

    // 운동 빈도를 체력 점수로 변환합니다.
    private int frequencyScore(ExerciseFrequency exerciseFrequency) {
        return switch (exerciseFrequency) {
            case DAILY -> 3;
            case WEEK_3_4, WEEK_1_2 -> 2;
            case MONTH_1_2, LESS_THAN_MONTH_1 -> 1;
        };
    }

    // 운동 시간을 체력 점수로 변환합니다.
    private int durationScore(ExerciseDuration exerciseDuration) {
        return switch (exerciseDuration) {
            case OVER_4H, HOUR_2_4 -> 3;
            case HOUR_1_2 -> 2;
            case UNDER_1H -> 1;
        };
    }
}
