package com.semosan.api.domain.oauth.service;

import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.common.jwt.TokenIssuance;
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

        // DTO 파싱은 oauth 레이어에서 처리 후 순수 값만 UserService로 전달
        KakaoUserInfoResponse.KakaoAccount account = userInfo.kakaoAccount();
        String kakaoId = userInfo.id().toString();
        String email = account != null ? account.email() : null;
        String name = account != null && account.profile() != null ? account.profile().nickname() : null;
        String profileUrl = account != null && account.profile() != null ? account.profile().profileImageUrl() : null;

        User user = userService.findOrRegisterKakaoUser(kakaoId, email, name, profileUrl, request.deviceType());

        TokenIssuance tokens = jwtService.issueTokens(user);
        return new OAuthLoginResponse(user.getId(), tokens.accessToken(), tokens.refreshToken());
    }

    @Transactional
    public OAuthLoginResponse appleLogin(OAuthAppleLoginRequest request) {
        Claims claims = oAuthAppleClient.getAppleClaims(request.identityToken());

        String appleId = claims.getSubject();
        String email = claims.get("email", String.class);

        User user = userService.findOrRegisterAppleUser(appleId, email, request.name(), request.deviceType());

        TokenIssuance tokens = jwtService.issueTokens(user);
        return new OAuthLoginResponse(user.getId(), tokens.accessToken(), tokens.refreshToken());
    }

}
