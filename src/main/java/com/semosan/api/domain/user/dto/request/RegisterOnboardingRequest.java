package com.semosan.api.domain.user.dto.request;

import com.semosan.api.domain.user.enums.onboarding.ExerciseDuration;
import com.semosan.api.domain.user.enums.onboarding.ExerciseFrequency;
import com.semosan.api.domain.user.enums.onboarding.ExerciseType;
import com.semosan.api.domain.user.enums.user.Gender;
import com.semosan.api.domain.user.enums.onboarding.HikingLevel;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterOnboardingRequest(
        @NotBlank
        @Size(min = 2, max = 10)
        String nickname,

        @Size(max = 255)
        String profileUrl,

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
        Boolean pushNotificationEnabled,

        @NotNull
        Boolean liveActivityEnabled,

        @NotNull
        Boolean voiceEnabled,

        @NotNull
        HikingLevel hikingLevel,

        @NotNull
        ExerciseType exerciseType,

        ExerciseFrequency exerciseFrequency,

        ExerciseDuration exerciseDuration
) {
}
