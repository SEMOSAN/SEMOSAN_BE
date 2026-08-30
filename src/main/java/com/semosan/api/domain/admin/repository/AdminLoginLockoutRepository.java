package com.semosan.api.domain.admin.repository;

import com.semosan.api.domain.admin.entity.AdminLoginLockout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AdminLoginLockoutRepository extends JpaRepository<AdminLoginLockout, String> {

    /**
     * username의 시도 횟수를 원자적으로 1 증가시키고 결과 카운트를 반환한다.
     * INSERT ... ON CONFLICT DO UPDATE는 짧은 row-level 락만 잡는 단일 문장이라
     * advisory lock과 달리 커넥션을 오래 붙잡지 않는다 — 동시 요청에도 풀 고갈 없음.
     * 기존 윈도우가 windowStart 이전에 시작됐으면 1로 리셋하며 새 윈도우를 시작한다.
     */
    @Query(value = """
            INSERT INTO admin_login_lockouts (username, fail_count, window_started_at)
            VALUES (:username, 1, now())
            ON CONFLICT (username) DO UPDATE SET
                fail_count = CASE WHEN admin_login_lockouts.window_started_at <= :windowStart
                                   THEN 1 ELSE admin_login_lockouts.fail_count + 1 END,
                window_started_at = CASE WHEN admin_login_lockouts.window_started_at <= :windowStart
                                   THEN now() ELSE admin_login_lockouts.window_started_at END
            RETURNING fail_count
            """, nativeQuery = true)
    int recordAttemptAndGetCount(@Param("username") String username, @Param("windowStart") LocalDateTime windowStart);

    // 로그인 성공 시 실패 카운터를 초기화한다.
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM admin_login_lockouts WHERE username = :username", nativeQuery = true)
    void reset(@Param("username") String username);
}
