package com.semosan.api.domain.auth.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.common.jwt.TokenIssuance;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.auth.dto.request.LoginRequest;
import com.semosan.api.domain.auth.dto.response.LoginResponse;
import com.semosan.api.domain.auth.dto.response.ReissueResponse;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.service.UserReader;
import com.semosan.api.domain.user.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final UserReader userReader;
    private final JwtService jwtService;

    @Value("${test.secret-key}")
    private String testSecretKey;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (!MessageDigest.isEqual(
                testSecretKey.getBytes(StandardCharsets.UTF_8),
                request.secretKey().getBytes(StandardCharsets.UTF_8))) {
            throw new GeneralException(ErrorStatus.FORBIDDEN);
        }

        User user = userService.findOrCreateTestUser(request.testUserId(), request.deviceType());
        TokenIssuance tokens = jwtService.issueTokens(user);

        return LoginResponse.from(user, tokens);
    }

    @Transactional
    public ReissueResponse reissue(String refreshToken) {
        Claims claims = jwtService.validateRefreshTokenSignature(refreshToken);
        Long userId = Long.parseLong(claims.getSubject());

        User user = userReader.findActiveUserById(userId);
        jwtService.validateRefreshToken(refreshToken, userId);

        TokenIssuance tokens = jwtService.issueTokens(user);
        return new ReissueResponse(tokens.accessToken(), tokens.refreshToken());
    }

    public void logout(Long userId, String accessToken) {
        jwtService.blacklistAccessToken(accessToken);
        jwtService.deleteRefreshToken(userId);
    }

    @Transactional
    public void withdraw(Long userId, String accessToken) {
        User user = userReader.findActiveUserById(userId);
        jwtService.blacklistAccessToken(accessToken);
        jwtService.deleteRefreshToken(userId);
        userService.withdrawUser(user);
    }

}
