package com.semosan.api.domain.user.dto.command;

import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.onboarding.ExerciseDuration;
import com.semosan.api.domain.user.enums.onboarding.ExerciseFrequency;
import com.semosan.api.domain.user.enums.onboarding.ExerciseType;
import com.semosan.api.domain.user.enums.onboarding.FitnessLevel;
import com.semosan.api.domain.user.enums.onboarding.HikingGoalType;
import com.semosan.api.domain.user.enums.onboarding.HikingLevel;
import com.semosan.api.domain.user.enums.onboarding.HikingPurpose;
import com.semosan.api.domain.user.enums.onboarding.PreferredDifficulty;

public record CreateUserOnboardingCommand(
        User user,
        HikingLevel hikingLevel,
        PreferredDifficulty preferredDifficulty,
        ExerciseType exerciseType,
        ExerciseFrequency exerciseFrequency,
        ExerciseDuration exerciseDuration,
        HikingGoalType hikingGoalType,
        HikingPurpose hikingPurpose,
        FitnessLevel fitnessLevel
) {
}
