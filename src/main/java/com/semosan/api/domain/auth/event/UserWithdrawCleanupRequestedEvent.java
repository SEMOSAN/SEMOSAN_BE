package com.semosan.api.domain.auth.event;

public record UserWithdrawCleanupRequestedEvent(Long userId, String accessToken) {
}
