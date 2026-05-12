package com.semosan.api.domain.user.dto.request;

import com.semosan.api.domain.user.enums.onboarding.ExerciseType;
import com.semosan.api.domain.user.enums.onboarding.HikingLevel;
import com.semosan.api.domain.user.enums.user.Gender;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateUserProfileRequest(
        @Size(max = 255)
        String profileUrl,

        @Size(min = 2, max = 10)
        String nickname,

        Gender gender,

        @Past
        LocalDate birthDate,

        @DecimalMin("50.0")
        @DecimalMax("250.0")
        Double height,

        @DecimalMin("20.0")
        @DecimalMax("300.0")
        Double weight,

        HikingLevel hikingLevel,

        ExerciseType exerciseType
) {
}
