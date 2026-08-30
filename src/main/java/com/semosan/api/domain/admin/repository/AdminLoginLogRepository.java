package com.semosan.api.domain.admin.repository;

import com.semosan.api.domain.admin.entity.AdminLoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AdminLoginLogRepository extends JpaRepository<AdminLoginLog, Long> {

    /**
     * 브루트포스 잠금 판정용 — "마지막 성공 로그인 이후"와 "잠금 윈도우 시작 시각" 중
     * 더 늦은 시점 이후의 실패 횟수만 센다.
     * 성공 로그인이 있으면 그 이전 실패는 무시되고(자동 초기화), 성공이 없으면
     * windowStart 이전 실패는 무시되어(윈도우 경과 시 자동 해제) 별도 리셋 로직이 필요 없다.
     */
    @Query(value = """
            SELECT COUNT(*) FROM admin_login_logs
            WHERE username = :username
              AND success = false
              AND attempted_at > GREATEST(
                    COALESCE(
                        (SELECT MAX(attempted_at) FROM admin_login_logs WHERE username = :username AND success = true),
                        :windowStart
                    ),
                    :windowStart
                  )
            """, nativeQuery = true)
    long countFailuresSinceLastSuccessOrWindowStart(
            @Param("username") String username,
            @Param("windowStart") LocalDateTime windowStart
    );
}
