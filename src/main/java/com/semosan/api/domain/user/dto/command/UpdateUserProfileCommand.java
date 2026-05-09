package com.semosan.api.domain.user.dto.command;

import com.semosan.api.domain.user.enums.user.Gender;

import java.time.LocalDate;

public record UpdateUserProfileCommand(
        String profileUrl,
        String nickname,
        Gender gender,
        LocalDate birthDate,
        Double height,
        Double weight
) {
}
