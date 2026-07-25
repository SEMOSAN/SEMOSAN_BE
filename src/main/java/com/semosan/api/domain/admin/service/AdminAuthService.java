package com.semosan.api.domain.admin.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.admin.dto.request.AdminLoginRequest;
import com.semosan.api.domain.admin.dto.response.AdminLoginResponse;
import com.semosan.api.domain.admin.entity.Admin;
import com.semosan.api.domain.admin.entity.AdminLoginLog;
import com.semosan.api.domain.admin.repository.AdminLoginLogRepository;
import com.semosan.api.domain.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminRepository adminRepository;
    private final AdminLoginLogRepository adminLoginLogRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request, String ipAddress, String userAgent) {
        Admin admin = adminRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    saveFailLog(request.username(), ipAddress, userAgent, "존재하지 않는 계정");
                    return new GeneralException(ErrorStatus.ADMIN_LOGIN_FAILED);
                });

        if (!passwordEncoder.matches(request.password(), admin.getPassword())) {
            saveFailLog(request.username(), ipAddress, userAgent, "비밀번호 불일치");
            throw new GeneralException(ErrorStatus.ADMIN_LOGIN_FAILED);
        }

        adminLoginLogRepository.save(AdminLoginLog.success(request.username(), ipAddress, userAgent));

        String accessToken = jwtService.generateAdminAccessToken(admin);
        return new AdminLoginResponse(admin.getId(), admin.getName(), accessToken);
    }

    private void saveFailLog(String username, String ipAddress, String userAgent, String reason) {
        adminLoginLogRepository.save(AdminLoginLog.fail(username, ipAddress, userAgent, reason));
    }
}
