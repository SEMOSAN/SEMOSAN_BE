package com.semosan.api.domain.admin.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.admin.dto.request.AdminLoginRequest;
import com.semosan.api.domain.admin.dto.response.AdminLoginResponse;
import com.semosan.api.domain.admin.entity.Admin;
import com.semosan.api.domain.admin.repository.AdminRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private AdminLoginLogService adminLoginLogService;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminAuthService adminAuthService;

    @Test
    void loginReturnsAccessTokenAndWritesSuccessLog() {
        Admin admin = mock(Admin.class);
        AdminLoginRequest request = new AdminLoginRequest("admin", "password");
        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(admin.getPassword()).thenReturn("encoded");
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        when(jwtService.generateAdminAccessToken(admin)).thenReturn("access-token");
        when(admin.getId()).thenReturn(1L);
        when(admin.getName()).thenReturn("관리자");

        AdminLoginResponse response = adminAuthService.login(request, "127.0.0.1", "JUnit");

        assertThat(response.adminId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("관리자");
        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(adminLoginLogService).saveSuccessLog("admin", "127.0.0.1", "JUnit");
        verify(adminLoginLogService, never()).saveFailLog(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void loginThrowsAndWritesFailLogWhenAdminDoesNotExist() {
        AdminLoginRequest request = new AdminLoginRequest("missing", "password");
        when(adminRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminAuthService.login(request, "127.0.0.1", "JUnit"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.ADMIN_LOGIN_FAILED);
        verify(adminLoginLogService).saveFailLog("missing", "127.0.0.1", "JUnit", "존재하지 않는 계정");
        verify(jwtService, never()).generateAdminAccessToken(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void loginThrowsAndWritesFailLogWhenPasswordDoesNotMatch() {
        Admin admin = mock(Admin.class);
        AdminLoginRequest request = new AdminLoginRequest("admin", "wrong");
        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(admin.getPassword()).thenReturn("encoded");
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> adminAuthService.login(request, "127.0.0.1", "JUnit"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.ADMIN_LOGIN_FAILED);
        verify(adminLoginLogService).saveFailLog("admin", "127.0.0.1", "JUnit", "비밀번호 불일치");
        verify(jwtService, never()).generateAdminAccessToken(admin);
    }
}
