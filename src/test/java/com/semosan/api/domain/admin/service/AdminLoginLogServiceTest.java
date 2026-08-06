package com.semosan.api.domain.admin.service;

import com.semosan.api.domain.admin.entity.AdminLoginLog;
import com.semosan.api.domain.admin.repository.AdminLoginLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminLoginLogServiceTest {

    @Mock
    private AdminLoginLogRepository adminLoginLogRepository;

    @InjectMocks
    private AdminLoginLogService adminLoginLogService;

    @Test
    void saveSuccessLogStoresSuccessfulAttempt() {
        adminLoginLogService.saveSuccessLog("admin", "127.0.0.1", "JUnit");

        ArgumentCaptor<AdminLoginLog> captor = ArgumentCaptor.forClass(AdminLoginLog.class);
        verify(adminLoginLogRepository).save(captor.capture());
        AdminLoginLog log = captor.getValue();
        assertThat(log.getUsername()).isEqualTo("admin");
        assertThat(log.isSuccess()).isTrue();
        assertThat(log.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(log.getUserAgent()).isEqualTo("JUnit");
        assertThat(log.getFailReason()).isNull();
        assertThat(log.getAttemptedAt()).isNotNull();
    }

    @Test
    void saveFailLogStoresFailedAttemptWithReason() {
        adminLoginLogService.saveFailLog("admin", "127.0.0.1", "JUnit", "비밀번호 불일치");

        ArgumentCaptor<AdminLoginLog> captor = ArgumentCaptor.forClass(AdminLoginLog.class);
        verify(adminLoginLogRepository).save(captor.capture());
        AdminLoginLog log = captor.getValue();
        assertThat(log.getUsername()).isEqualTo("admin");
        assertThat(log.isSuccess()).isFalse();
        assertThat(log.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(log.getUserAgent()).isEqualTo("JUnit");
        assertThat(log.getFailReason()).isEqualTo("비밀번호 불일치");
        assertThat(log.getAttemptedAt()).isNotNull();
    }
}
