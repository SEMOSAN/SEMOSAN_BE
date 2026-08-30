package com.semosan.api.domain.admin.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.admin.dto.request.AdminLoginRequest;
import com.semosan.api.domain.admin.dto.response.AdminLoginResponse;
import com.semosan.api.domain.admin.entity.Admin;
import com.semosan.api.domain.admin.repository.AdminLoginLogRepository;
import com.semosan.api.domain.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final AdminLoginLogRepository adminLoginLogRepository;
    private final AdminLoginLogService adminLoginLogService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public AdminLoginResponse login(AdminLoginRequest request, String ipAddress, String userAgent) {
        LocalDateTime windowStart = LocalDateTime.now().minus(LOCKOUT_WINDOW);
        long recentFailures = adminLoginLogRepository.countFailuresSinceLastSuccessOrWindowStart(
                request.username(), windowStart);
        if (recentFailures >= MAX_LOGIN_FAILURES) {
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

        adminLoginLogService.saveSuccessLog(request.username(), ipAddress, userAgent);

        String accessToken = jwtService.generateAdminAccessToken(admin);
        return new AdminLoginResponse(admin.getId(), admin.getName(), accessToken);
    }
}
