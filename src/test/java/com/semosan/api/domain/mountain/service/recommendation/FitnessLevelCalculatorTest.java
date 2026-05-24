package com.semosan.api.domain.mountain.service.recommendation;

import com.semosan.api.domain.mountain.enums.FitnessLevel;
import com.semosan.api.domain.user.dto.command.CompleteOnboardingCommand;
import com.semosan.api.domain.user.dto.command.CreateUserOnboardingCommand;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.entity.UserOnboarding;
import com.semosan.api.domain.user.enums.onboarding.ExerciseDuration;
import com.semosan.api.domain.user.enums.onboarding.ExerciseFrequency;
import com.semosan.api.domain.user.enums.onboarding.ExerciseType;
import com.semosan.api.domain.user.enums.onboarding.HikingLevel;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.enums.user.Gender;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FitnessLevelCalculatorTest {

    private final FitnessLevelCalculator calculator = new FitnessLevelCalculator();

    @Test
    void calculatesAdvancedLevelWithHighExperienceAndExerciseHabit() {
        User user = user(Gender.MALE, 175.0, 70.0);
        UserOnboarding onboarding = onboarding(
                user,
                HikingLevel.EXPERT,
                ExerciseType.HIKING,
                ExerciseFrequency.DAILY,
                ExerciseDuration.OVER_4H
        );

        assertThat(calculator.calculate(user, onboarding)).isEqualTo(FitnessLevel.ADVANCED);
    }

    @Test
    void capsBeginnerHikingExperienceAtBeginnerLevel() {
        User user = user(Gender.MALE, 175.0, 70.0);
        UserOnboarding onboarding = onboarding(
                user,
                HikingLevel.BEGINNER,
                ExerciseType.CROSSFIT,
                ExerciseFrequency.DAILY,
                ExerciseDuration.OVER_4H
        );

        assertThat(calculator.calculate(user, onboarding)).isEqualTo(FitnessLevel.BEGINNER);
    }

    @Test
    void fixesExerciseNoneToEntryLevel() {
        User user = user(Gender.FEMALE, 165.0, 55.0);
        UserOnboarding onboarding = onboarding(
                user,
                HikingLevel.EXPERT,
                ExerciseType.NONE,
                null,
                null
        );

        assertThat(calculator.calculate(user, onboarding)).isEqualTo(FitnessLevel.ENTRY);
    }

    private User user(Gender gender, Double height, Double weight) {
        User user = User.createTestUser("fitness-test-user", DeviceType.IOS);
        user.completeOnboarding(new CompleteOnboardingCommand(
                "테스트",
                null,
                LocalDate.of(1990, 1, 1),
                gender,
                height,
                weight
        ));
        return user;
    }

    private UserOnboarding onboarding(
            User user,
            HikingLevel hikingLevel,
            ExerciseType exerciseType,
            ExerciseFrequency exerciseFrequency,
            ExerciseDuration exerciseDuration
    ) {
        return UserOnboarding.create(new CreateUserOnboardingCommand(
                user,
                hikingLevel,
                exerciseType,
                exerciseFrequency,
                exerciseDuration
        ));
    }
}
