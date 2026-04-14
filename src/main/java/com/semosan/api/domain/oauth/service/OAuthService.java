package com.semosan.api.domain.oauth.service;

import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.domain.oauth.client.OAuthAppleClient;
import com.semosan.api.domain.oauth.client.OAuthKakaoClient;
import com.semosan.api.domain.oauth.dto.KakaoTokenResponse;
import com.semosan.api.domain.oauth.dto.KakaoUserInfoResponse;
import com.semosan.api.domain.oauth.dto.request.OAuthAppleLoginRequest;
import com.semosan.api.domain.oauth.dto.request.OAuthKakaoLoginRequest;
import com.semosan.api.domain.oauth.dto.response.OAuthLoginResponse;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final OAuthKakaoClient oAuthKakaoClient;
    private final OAuthAppleClient oAuthAppleClient;

    @Transactional
    public OAuthLoginResponse kakaoLogin(OAuthKakaoLoginRequest request) {
        KakaoTokenResponse kakaoToken = oAuthKakaoClient.getKakaoToken(request.code());
        KakaoUserInfoResponse userInfo = oAuthKakaoClient.getKakaoUserInfo(kakaoToken.accessToken());

        User user = userService.findOrRegisterKakaoUser(userInfo, request.deviceType());
        return issueTokens(user);
    }

    @Transactional
    public OAuthLoginResponse appleLogin(OAuthAppleLoginRequest request) {
        Claims claims = oAuthAppleClient.getAppleClaims(request.identityToken());

        String appleId = claims.getSubject();
        String email = claims.get("email", String.class);

        User user = userService.findOrRegisterAppleUser(appleId, email, request.name(), request.deviceType());
        return issueTokens(user);
    }

    private OAuthLoginResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        user.updateRefreshToken(jwtService.hashToken(refreshToken));

        return new OAuthLoginResponse(user.getId(), accessToken, refreshToken);
    }

}
