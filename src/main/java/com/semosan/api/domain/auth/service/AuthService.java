package com.semosan.api.domain.auth.service;

import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.domain.auth.dto.request.LoginRequest;
import com.semosan.api.domain.auth.dto.response.LoginResponse;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;

    @Transactional
    public LoginResponse login(LoginRequest request) {

        User user = userService.findOrCreateTestUser(request.testUserId(), request.deviceType());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        user.updateRefreshToken(jwtService.hashToken(refreshToken));

        return new LoginResponse(user.getId(), accessToken, refreshToken);
    }

}
