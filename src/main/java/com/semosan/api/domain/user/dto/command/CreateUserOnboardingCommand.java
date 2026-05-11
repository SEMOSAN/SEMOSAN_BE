package com.semosan.api.domain.user.dto.command;

import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.onboarding.ExerciseDuration;
import com.semosan.api.domain.user.enums.onboarding.ExerciseFrequency;
import com.semosan.api.domain.user.enums.onboarding.ExerciseType;
import com.semosan.api.domain.user.enums.onboarding.HikingLevel;

public record CreateUserOnboardingCommand(
        User user,
        HikingLevel hikingLevel,
        ExerciseType exerciseType,
        ExerciseFrequency exerciseFrequency,
        ExerciseDuration exerciseDuration
) {
}
