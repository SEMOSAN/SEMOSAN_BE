package com.semosan.api.domain.admin.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AdminUserSuspendRequest(
        @NotNull @Future LocalDateTime suspendedUntil
) {
}
