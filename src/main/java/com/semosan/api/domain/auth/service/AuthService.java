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

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;

    @Value("${test.secret-key}")
    private String testSecretKey;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (!testSecretKey.equals(request.secretKey()))
            throw new GeneralException(ErrorStatus.FORBIDDEN);

        User user = userService.findOrCreateTestUser(request.testUserId(), request.deviceType());
        log.warn("[TEST] 테스트 로그인 testUserId={}, userId={}", request.testUserId(), user.getId());

        TokenPair tokens = issueTokens(user);
        return new LoginResponse(user.getId(), tokens.accessToken(), tokens.refreshToken());
    }

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

    // 토큰 발급 및 리프레시 토큰 해시 저장
    private TokenPair issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        user.updateRefreshToken(jwtService.hashToken(refreshToken));

        return new TokenPair(accessToken, refreshToken);
    }

    private record TokenPair(String accessToken, String refreshToken) {}

}
