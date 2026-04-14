package com.semosan.api.domain.user.service;

import com.semosan.api.domain.oauth.dto.KakaoUserInfoResponse;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.DeviceType;
import com.semosan.api.domain.user.enums.OAuthProvider;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User findOrRegisterKakaoUser(KakaoUserInfoResponse userInfo, DeviceType deviceType) {
        KakaoUserInfoResponse.KakaoAccount account = userInfo.kakaoAccount();
        String kakaoId = userInfo.id().toString();

        String email = account != null ? account.email() : null;
        String name = account != null && account.profile() != null ? account.profile().nickname() : null;
        String profileUrl = account != null && account.profile() != null ? account.profile().profileImageUrl() : null;

        return userRepository.findByOauthIdAndOauthProvider(kakaoId, OAuthProvider.KAKAO)
                .map(user -> {
                    if (user.isDeleted())
                        user.restore(email, name, profileUrl, deviceType);
                    return user;
                })
                .orElseGet(() ->
                        userRepository.save(User.createKakaoUser(kakaoId, email, name, profileUrl, deviceType))
                );
    }

}
