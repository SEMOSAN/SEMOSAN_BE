package com.semosan.api.domain.user.dto.command;

import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.ExerciseDuration;
import com.semosan.api.domain.user.enums.ExerciseFrequency;
import com.semosan.api.domain.user.enums.ExerciseType;
import com.semosan.api.domain.user.enums.FitnessLevel;
import com.semosan.api.domain.user.enums.HikingGoalType;
import com.semosan.api.domain.user.enums.HikingLevel;
import com.semosan.api.domain.user.enums.HikingPurpose;
import com.semosan.api.domain.user.enums.PreferredDifficulty;

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
