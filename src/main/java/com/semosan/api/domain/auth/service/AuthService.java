package com.semosan.api.domain.auth.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.auth.dto.request.LoginRequest;
import com.semosan.api.domain.auth.dto.response.LoginResponse;
import com.semosan.api.domain.auth.dto.response.ReissueResponse;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;

    @Value("${test.secret-key}")
    private String testSecretKey;

    // 시크릿 키 검증 후 테스트 유저 조회/생성 및 JWT 발급
    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (!MessageDigest.isEqual(
                testSecretKey.getBytes(StandardCharsets.UTF_8),
                request.secretKey().getBytes(StandardCharsets.UTF_8))) {
            throw new GeneralException(ErrorStatus.FORBIDDEN);
        }

        User user = userService.findOrCreateTestUser(request.testUserId(), request.deviceType());
        log.warn("[TEST] 테스트 로그인 testUserId={}, userId={}", request.testUserId(), user.getId());

        TokenPair tokens = issueTokens(user);
        return new LoginResponse(user.getId(), tokens.accessToken(), tokens.refreshToken());
    }

    // 리프레시 토큰 검증 후 새 액세스/리프레시 토큰 발급 (Rotation)
    @Transactional
    public ReissueResponse reissue(String refreshToken) {
        Claims claims = jwtService.validateRefreshTokenSignature(refreshToken);
        Long userId = Long.parseLong(claims.getSubject());

        User user = userService.findById(userId);

        if (user.getRefreshToken() == null)
            throw new GeneralException(ErrorStatus.REFRESH_TOKEN_NOT_FOUND);
        jwtService.validateRefreshToken(refreshToken, user.getRefreshToken());

        TokenPair tokens = issueTokens(user);
        return new ReissueResponse(tokens.accessToken(), tokens.refreshToken());
    }

    // 유저 조회 후 soft delete 및 리프레시 토큰 무효화
    @Transactional
    public void withdraw(Long userId) {
        User user = userService.findById(userId);
        user.withdraw();
    }

    // 액세스/리프레시 토큰 발급 및 리프레시 토큰 해시 저장
    private TokenPair issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        user.updateRefreshToken(jwtService.hashToken(refreshToken));
        return new TokenPair(accessToken, refreshToken);
    }

    private record TokenPair(String accessToken, String refreshToken) {}

}
