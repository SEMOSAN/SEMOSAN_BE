package com.semosan.api.domain.oauth.service;

import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.common.jwt.TokenIssuance;
import com.semosan.api.domain.notification.service.FcmTokenService;
import com.semosan.api.domain.oauth.dto.response.OAuthLoginResponse;
import com.semosan.api.domain.user.dto.command.OAuthUserProfile;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.enums.user.OAuthProvider;
import com.semosan.api.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthLoginProcessor {

    private final UserService userService;
    private final JwtService jwtService;
    private final FcmTokenService fcmTokenService;

    @Transactional
    public OAuthLoginResponse login(
            OAuthUserProfile profile,
            OAuthProvider provider,
            DeviceType deviceType
    ) {
        User user = userService.findOrRegisterOAuthUser(profile, provider, deviceType);
        TokenIssuance tokens = jwtService.issueTokens(user);
        // 단일 세션이라 새 로그인은 이전 기기의 세션을 무효화한다.
        // 그 기기의 FCM 토큰을 남겨두면 로그아웃된 기기 잠금화면에 알림 내용이 계속 뜬다.
        fcmTokenService.deleteAllByUserId(user.getId());
        return OAuthLoginResponse.of(user, tokens);
    }

}
