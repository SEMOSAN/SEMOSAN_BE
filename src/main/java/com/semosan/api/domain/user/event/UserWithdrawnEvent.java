package com.semosan.api.domain.user.event;

// 탈퇴 처리(user.withdraw()) 직후 발행되어, 각 도메인이 자신의 하위 데이터를 정리하도록 알린다.
// BEFORE_COMMIT 리스너로 소비되어 탈퇴 트랜잭션과 원자적으로 처리된다.
public record UserWithdrawnEvent(Long userId) {
}
