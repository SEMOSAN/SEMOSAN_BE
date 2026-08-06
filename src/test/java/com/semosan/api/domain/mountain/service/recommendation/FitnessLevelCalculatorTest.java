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

    @Test
    void treatsMissingExerciseFrequencyOrDurationAsZeroExerciseScore() {
        User user = user(Gender.MALE, 175.0, 70.0);
        UserOnboarding missingFrequency = onboarding(
                user,
                HikingLevel.EXPERIENCED,
                ExerciseType.RUNNING,
                null,
                ExerciseDuration.HOUR_1_2
        );
        UserOnboarding missingDuration = onboarding(
                user,
                HikingLevel.EXPERIENCED,
                ExerciseType.RUNNING,
                ExerciseFrequency.WEEK_1_2,
                null
        );

        assertThat(calculator.calculate(user, missingFrequency)).isEqualTo(FitnessLevel.ENTRY);
        assertThat(calculator.calculate(user, missingDuration)).isEqualTo(FitnessLevel.ENTRY);
    }

    @Test
    void raisesLowScoringExpertToIntermediate() {
        User user = user(Gender.NONE, null, null);
        UserOnboarding onboarding = onboarding(
                user,
                HikingLevel.EXPERT,
                ExerciseType.WALKING,
                ExerciseFrequency.LESS_THAN_MONTH_1,
                ExerciseDuration.UNDER_1H
        );

        assertThat(calculator.calculate(user, onboarding)).isEqualTo(FitnessLevel.INTERMEDIATE);
    }

    @Test
    void appliesMaleBmiAdjustmentRanges() {
        assertThat(calculator.calculate(
                user(Gender.MALE, 175.0, 70.0),
                onboarding(user(Gender.MALE, 175.0, 70.0), HikingLevel.HOBBY, ExerciseType.GYM,
                        ExerciseFrequency.WEEK_3_4, ExerciseDuration.HOUR_2_4)
        )).isEqualTo(FitnessLevel.INTERMEDIATE);

        assertThat(calculator.calculate(
                user(Gender.MALE, 175.0, 81.0),
                onboarding(user(Gender.MALE, 175.0, 81.0), HikingLevel.HOBBY, ExerciseType.GYM,
                        ExerciseFrequency.WEEK_3_4, ExerciseDuration.HOUR_2_4)
        )).isEqualTo(FitnessLevel.INTERMEDIATE);

        assertThat(calculator.calculate(
                user(Gender.MALE, 175.0, 58.0),
                onboarding(user(Gender.MALE, 175.0, 58.0), HikingLevel.EXPERIENCED, ExerciseType.SPORTS,
                        ExerciseFrequency.MONTH_1_2, ExerciseDuration.UNDER_1H)
        )).isEqualTo(FitnessLevel.ENTRY);

        assertThat(calculator.calculate(
                user(Gender.MALE, 175.0, 95.0),
                onboarding(user(Gender.MALE, 175.0, 95.0), HikingLevel.EXPERIENCED, ExerciseType.SPORTS,
                        ExerciseFrequency.MONTH_1_2, ExerciseDuration.UNDER_1H)
        )).isEqualTo(FitnessLevel.ENTRY);
    }

    @Test
    void appliesFemaleBmiAdjustmentRanges() {
        assertThat(calculator.calculate(
                user(Gender.FEMALE, 165.0, 55.0),
                onboarding(user(Gender.FEMALE, 165.0, 55.0), HikingLevel.HOBBY, ExerciseType.GYM,
                        ExerciseFrequency.WEEK_3_4, ExerciseDuration.HOUR_2_4)
        )).isEqualTo(FitnessLevel.INTERMEDIATE);

        assertThat(calculator.calculate(
                user(Gender.FEMALE, 165.0, 65.0),
                onboarding(user(Gender.FEMALE, 165.0, 65.0), HikingLevel.HOBBY, ExerciseType.GYM,
                        ExerciseFrequency.WEEK_3_4, ExerciseDuration.HOUR_2_4)
        )).isEqualTo(FitnessLevel.INTERMEDIATE);

        assertThat(calculator.calculate(
                user(Gender.FEMALE, 165.0, 48.0),
                onboarding(user(Gender.FEMALE, 165.0, 48.0), HikingLevel.EXPERIENCED, ExerciseType.SPORTS,
                        ExerciseFrequency.MONTH_1_2, ExerciseDuration.UNDER_1H)
        )).isEqualTo(FitnessLevel.ENTRY);

        assertThat(calculator.calculate(
                user(Gender.FEMALE, 165.0, 85.0),
                onboarding(user(Gender.FEMALE, 165.0, 85.0), HikingLevel.EXPERIENCED, ExerciseType.SPORTS,
                        ExerciseFrequency.MONTH_1_2, ExerciseDuration.UNDER_1H)
        )).isEqualTo(FitnessLevel.ENTRY);
    }

    @Test
    void ignoresBmiAdjustmentWhenProfileIsIncompleteOrHeightInvalid() {
        assertThat(calculator.calculate(
                user(Gender.NONE, 175.0, 70.0),
                onboarding(user(Gender.NONE, 175.0, 70.0), HikingLevel.EXPERIENCED, ExerciseType.SPORTS,
                        ExerciseFrequency.MONTH_1_2, ExerciseDuration.UNDER_1H)
        )).isEqualTo(FitnessLevel.ENTRY);

        assertThat(calculator.calculate(
                user(Gender.MALE, 0.0, 70.0),
                onboarding(user(Gender.MALE, 0.0, 70.0), HikingLevel.EXPERIENCED, ExerciseType.SPORTS,
                        ExerciseFrequency.MONTH_1_2, ExerciseDuration.UNDER_1H)
        )).isEqualTo(FitnessLevel.ENTRY);
    }

    @Test
    void appliesAgeAdjustmentRanges() {
        User teenager = user(Gender.MALE, 175.0, 70.0, LocalDate.now().minusYears(16));
        User youngAdult = user(Gender.MALE, 175.0, 70.0, LocalDate.now().minusYears(25));
        User middleAged = user(Gender.MALE, 175.0, 70.0, LocalDate.now().minusYears(45));
        User older = user(Gender.MALE, 175.0, 70.0, LocalDate.now().minusYears(60));

        assertThat(calculator.calculate(teenager, baselineOnboarding(teenager))).isEqualTo(FitnessLevel.BEGINNER);
        assertThat(calculator.calculate(youngAdult, baselineOnboarding(youngAdult))).isEqualTo(FitnessLevel.BEGINNER);
        assertThat(calculator.calculate(middleAged, baselineOnboarding(middleAged))).isEqualTo(FitnessLevel.BEGINNER);
        assertThat(calculator.calculate(older, baselineOnboarding(older))).isEqualTo(FitnessLevel.BEGINNER);
    }

    private User user(Gender gender, Double height, Double weight) {
        return user(gender, height, weight, LocalDate.of(1990, 1, 1));
    }

    private User user(Gender gender, Double height, Double weight, LocalDate birthDate) {
        User user = User.createTestUser("fitness-test-user", DeviceType.IOS);
        user.completeOnboarding(new CompleteOnboardingCommand(
                "테스트",
                null,
                birthDate,
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

    private UserOnboarding baselineOnboarding(User user) {
        return onboarding(
                user,
                HikingLevel.EXPERIENCED,
                ExerciseType.HIKING,
                ExerciseFrequency.WEEK_1_2,
                ExerciseDuration.HOUR_1_2
        );
    }
}
