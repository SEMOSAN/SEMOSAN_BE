package com.semosan.api.domain.user.dto.request;

import com.semosan.api.domain.user.enums.ExerciseDuration;
import com.semosan.api.domain.user.enums.ExerciseFrequency;
import com.semosan.api.domain.user.enums.ExerciseType;
import com.semosan.api.domain.user.enums.Gender;
import com.semosan.api.domain.user.enums.HikingGoalType;
import com.semosan.api.domain.user.enums.HikingLevel;
import com.semosan.api.domain.user.enums.HikingPurpose;
import com.semosan.api.domain.user.enums.PreferredDifficulty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterOnboardingRequest(
        @NotBlank
        @Size(max = 30)
        String nickname,

        @NotNull
        @Past
        LocalDate birthDate,

        @NotNull
        Gender gender,

        @NotNull
        @DecimalMin("50.0")
        @DecimalMax("250.0")
        Double height,

        @NotNull
        @DecimalMin("20.0")
        @DecimalMax("300.0")
        Double weight,

        @NotNull
        HikingLevel hikingLevel,

        PreferredDifficulty preferredDifficulty,

        @NotNull
        ExerciseType exerciseType,

        @NotNull
        ExerciseFrequency exerciseFrequency,

        @NotNull
        ExerciseDuration exerciseDuration,

        @NotNull
        HikingGoalType hikingGoalType,

        @NotNull
        HikingPurpose hikingPurpose
) {
}
