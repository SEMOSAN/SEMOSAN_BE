package com.semosan.api.domain.user.dto.command;

import com.semosan.api.domain.user.enums.user.Gender;

import java.time.LocalDate;

public record CompleteOnboardingCommand(
        String nickname,
        LocalDate birthDate,
        Gender gender,
        Double height,
        Double weight
) {
}
