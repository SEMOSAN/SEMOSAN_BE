package com.semosan.api.domain.admin.service;

import com.semosan.api.domain.admin.entity.AdminLoginLog;
import com.semosan.api.domain.admin.repository.AdminLoginLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminLoginLogService {

    private final AdminLoginLogRepository adminLoginLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSuccessLog(String username, String ipAddress, String userAgent) {
        adminLoginLogRepository.save(AdminLoginLog.success(username, ipAddress, userAgent));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailLog(String username, String ipAddress, String userAgent, String reason) {
        adminLoginLogRepository.save(AdminLoginLog.fail(username, ipAddress, userAgent, reason));
    }
}
