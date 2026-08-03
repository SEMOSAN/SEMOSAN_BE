package com.semosan.api.domain.oauth.service;

import com.semosan.api.domain.oauth.client.OAuthAppleClient;
import com.semosan.api.domain.oauth.client.OAuthKakaoClient;
import com.semosan.api.domain.oauth.dto.KakaoUserInfoResponse;
import com.semosan.api.domain.oauth.dto.request.OAuthAppleLoginRequest;
import com.semosan.api.domain.oauth.dto.request.OAuthKakaoLoginRequest;
import com.semosan.api.domain.oauth.dto.response.OAuthLoginResponse;
import com.semosan.api.domain.user.dto.command.OAuthUserProfile;
import com.semosan.api.domain.user.enums.user.OAuthProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final OAuthKakaoClient oAuthKakaoClient;
    private final OAuthAppleClient oAuthAppleClient;
    private final OAuthLoginProcessor oAuthLoginProcessor;

    public OAuthLoginResponse kakaoLogin(OAuthKakaoLoginRequest request) {
        KakaoUserInfoResponse userInfo = oAuthKakaoClient.getKakaoUserInfo(request.accessToken());
        OAuthUserProfile profile = toKakaoOAuthUserProfile(userInfo);

        return oAuthLoginProcessor.login(profile, OAuthProvider.KAKAO, request.deviceType());
    }

    public OAuthLoginResponse appleLogin(OAuthAppleLoginRequest request) {
        Claims claims = oAuthAppleClient.getAppleClaims(request.identityToken());
        OAuthUserProfile profile = toAppleOAuthUserProfile(claims, request);

        return oAuthLoginProcessor.login(profile, OAuthProvider.APPLE, request.deviceType());
    }

    private OAuthUserProfile toKakaoOAuthUserProfile(KakaoUserInfoResponse userInfo) {
        KakaoUserInfoResponse.KakaoAccount account = userInfo.kakaoAccount();
        KakaoUserInfoResponse.KakaoAccount.Profile profile = account != null ? account.profile() : null;

        return new OAuthUserProfile(
                userInfo.id().toString(),
                account != null ? account.email() : null,
                profile != null ? profile.nickname() : null
        );
    }

    private OAuthUserProfile toAppleOAuthUserProfile(Claims claims, OAuthAppleLoginRequest request) {
        return new OAuthUserProfile(
                claims.getSubject(),
                claims.get("email", String.class),
                request.name()
        );
    }

}
