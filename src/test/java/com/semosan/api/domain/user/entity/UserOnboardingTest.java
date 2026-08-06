package com.semosan.api.domain.user.entity;

import com.semosan.api.domain.user.dto.command.CreateUserOnboardingCommand;
import com.semosan.api.domain.user.enums.onboarding.ExerciseDuration;
import com.semosan.api.domain.user.enums.onboarding.ExerciseFrequency;
import com.semosan.api.domain.user.enums.onboarding.ExerciseType;
import com.semosan.api.domain.user.enums.onboarding.HikingLevel;
import com.semosan.api.domain.user.enums.user.DeviceType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserOnboardingTest {

    @Test
    void createInitializesFieldsFromCommand() {
        User user = user();

        UserOnboarding onboarding = UserOnboarding.create(new CreateUserOnboardingCommand(
                user,
                HikingLevel.EXPERT,
                ExerciseType.HIKING,
                ExerciseFrequency.DAILY,
                ExerciseDuration.OVER_4H
        ));

        assertThat(onboarding.getUser()).isSameAs(user);
        assertThat(onboarding.getHikingLevel()).isEqualTo(HikingLevel.EXPERT);
        assertThat(onboarding.getExerciseType()).isEqualTo(ExerciseType.HIKING);
        assertThat(onboarding.getExerciseFrequency()).isEqualTo(ExerciseFrequency.DAILY);
        assertThat(onboarding.getExerciseDuration()).isEqualTo(ExerciseDuration.OVER_4H);
    }

    @Test
    void updateHikingLevelChangesHikingLevel() {
        UserOnboarding onboarding = onboarding(ExerciseType.RUNNING);

        onboarding.updateHikingLevel(HikingLevel.BEGINNER);

        assertThat(onboarding.getHikingLevel()).isEqualTo(HikingLevel.BEGINNER);
    }

    @Test
    void updateExerciseTypeChangesTypeAndKeepsDetailsWhenTypeIsNotNone() {
        UserOnboarding onboarding = onboarding(ExerciseType.RUNNING);

        onboarding.updateExerciseType(ExerciseType.HIKING);

        assertThat(onboarding.getExerciseType()).isEqualTo(ExerciseType.HIKING);
        assertThat(onboarding.getExerciseFrequency()).isEqualTo(ExerciseFrequency.WEEK_3_4);
        assertThat(onboarding.getExerciseDuration()).isEqualTo(ExerciseDuration.HOUR_1_2);
    }

    @Test
    void updateExerciseTypeClearsDetailsWhenTypeIsNone() {
        UserOnboarding onboarding = onboarding(ExerciseType.RUNNING);

        onboarding.updateExerciseType(ExerciseType.NONE);

        assertThat(onboarding.getExerciseType()).isEqualTo(ExerciseType.NONE);
        assertThat(onboarding.getExerciseFrequency()).isNull();
        assertThat(onboarding.getExerciseDuration()).isNull();
    }

    private UserOnboarding onboarding(ExerciseType exerciseType) {
        return UserOnboarding.create(new CreateUserOnboardingCommand(
                user(),
                HikingLevel.EXPERIENCED,
                exerciseType,
                ExerciseFrequency.WEEK_3_4,
                ExerciseDuration.HOUR_1_2
        ));
    }

    private User user() {
        return User.createTestUser("onboarding-user", DeviceType.IOS);
    }
}
