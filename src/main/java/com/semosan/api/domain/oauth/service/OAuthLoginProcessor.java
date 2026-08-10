package com.semosan.api.domain.oauth.service;

import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.common.jwt.TokenIssuance;
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

    @Transactional
    public OAuthLoginResponse login(
            OAuthUserProfile profile,
            OAuthProvider provider,
            DeviceType deviceType
    ) {
        User user = userService.findOrRegisterOAuthUser(profile, provider, deviceType);
        TokenIssuance tokens = jwtService.issueTokens(user);
        return OAuthLoginResponse.of(user, tokens);
    }

}
