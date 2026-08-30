package com.semosan.api.domain.admin.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.admin.dto.request.AdminLoginRequest;
import com.semosan.api.domain.admin.dto.response.AdminLoginResponse;
import com.semosan.api.domain.admin.entity.Admin;
import com.semosan.api.domain.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    // #353: 연속 실패 MAX_LOGIN_FAILURES회 시 LOCKOUT_WINDOW 동안 잠금.
    // bcrypt로 해시된 비밀번호를 이 정도 시도 수로 뚫을 수는 없고, 윈도우가 지나면 다시
    // 같은 횟수만큼 허용되는 구조 자체가 시간당 시도 총량을 억제하는 핵심 방어선이다.
    private static final int MAX_LOGIN_FAILURES = 10;
    private static final Duration LOCKOUT_WINDOW = Duration.ofMinutes(15);

    private final AdminRepository adminRepository;
    private final AdminLoginLockoutService adminLoginLockoutService;
    private final AdminLoginLogService adminLoginLogService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    // login() 자체는 트랜잭션을 걸지 않는다 — 잠금 카운터/로그 기록이 전부 REQUIRES_NEW로
    // 독립 커밋되어야, 이 메서드가 실패로 예외를 던져도(=트랜잭션이 있었다면 롤백될 상황) 그
    // 기록들이 함께 사라지지 않는다. 동시에 outer 트랜잭션이 커넥션을 쥔 채로 REQUIRES_NEW가
    // 새 커넥션을 기다리는 자기잠금(커넥션 풀 고갈) 구조도 함께 사라진다.
    public AdminLoginResponse login(AdminLoginRequest request, String ipAddress, String userAgent) {
        // 시도마다 먼저 원자적으로 증가시키고 결과 카운트로 즉시 판정한다 — 단일 UPSERT라
        // advisory lock과 달리 커넥션을 오래 붙잡지 않아 동시 요청에도 풀 고갈이 없다.
        LocalDateTime windowStart = LocalDateTime.now().minus(LOCKOUT_WINDOW);
        int attemptCount = adminLoginLockoutService.recordAttempt(request.username(), windowStart);
        if (attemptCount > MAX_LOGIN_FAILURES) {
            throw new GeneralException(ErrorStatus.TOO_MANY_REQUESTS);
        }

        Admin admin = adminRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    adminLoginLogService.saveFailLog(request.username(), ipAddress, userAgent, "존재하지 않는 계정");
                    return new GeneralException(ErrorStatus.ADMIN_LOGIN_FAILED);
                });

        if (!passwordEncoder.matches(request.password(), admin.getPassword())) {
            adminLoginLogService.saveFailLog(request.username(), ipAddress, userAgent, "비밀번호 불일치");
            throw new GeneralException(ErrorStatus.ADMIN_LOGIN_FAILED);
        }

        adminLoginLockoutService.reset(request.username());
        adminLoginLogService.saveSuccessLog(request.username(), ipAddress, userAgent);

        String accessToken = jwtService.generateAdminAccessToken(admin);
        return new AdminLoginResponse(admin.getId(), admin.getName(), accessToken);
    }
}
