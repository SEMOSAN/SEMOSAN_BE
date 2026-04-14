package com.semosan.api.domain.oauth.service;

import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.domain.oauth.dto.request.OAuthKakaoLoginRequest;
import com.semosan.api.domain.oauth.dto.KakaoTokenResponse;
import com.semosan.api.domain.oauth.dto.KakaoUserInfoResponse;
import com.semosan.api.domain.oauth.dto.response.OAuthLoginResponse;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.service.UserService;
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
    private final OAuthKakaoService oAuthKakaoService;

    @Transactional
    public OAuthLoginResponse kakaoLogin(OAuthKakaoLoginRequest request) {

        KakaoTokenResponse kakaoToken = oAuthKakaoService.getKakaoToken(request.code());
        KakaoUserInfoResponse userInfo = oAuthKakaoService.getKakaoUserInfo(kakaoToken.accessToken());

        User user = userService.findOrRegisterKakaoUser(userInfo, request.deviceType());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        user.updateRefreshToken(jwtService.hashToken(refreshToken));
        return new OAuthLoginResponse(user.getId(), accessToken, refreshToken);
    }

}
