package com.semosan.api.domain.admin.service;

import com.semosan.api.domain.admin.repository.AdminLoginLockoutRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminLoginLockoutServiceTest {

    @Mock
    private AdminLoginLockoutRepository adminLoginLockoutRepository;

    @InjectMocks
    private AdminLoginLockoutService adminLoginLockoutService;

    @Test
    void recordAttemptDelegatesToRepositoryAndReturnsCount() {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(15);
        when(adminLoginLockoutRepository.recordAttemptAndGetCount("admin", windowStart)).thenReturn(3);

        int count = adminLoginLockoutService.recordAttempt("admin", windowStart);

        assertThat(count).isEqualTo(3);
        verify(adminLoginLockoutRepository).recordAttemptAndGetCount("admin", windowStart);
    }

    @Test
    void resetDelegatesToRepository() {
        adminLoginLockoutService.reset("admin");

        verify(adminLoginLockoutRepository).reset("admin");
    }
}
