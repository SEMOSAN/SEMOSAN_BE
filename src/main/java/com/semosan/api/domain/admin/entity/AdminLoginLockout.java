package com.semosan.api.domain.admin.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 관리자 로그인 브루트포스 잠금(#353)용 실패 카운터.
 * AdminLoginLogRepository의 원자적 UPSERT로만 갱신되고, 이 엔티티는 조회 매핑 용도.
 */
@Entity
@Table(name = "admin_login_lockouts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminLoginLockout {

    @Id
    private String username;

    private int failCount;

    private LocalDateTime windowStartedAt;
}
