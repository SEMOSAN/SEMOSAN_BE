package com.semosan.api.domain.user.service;

import com.semosan.api.domain.oauth.dto.KakaoUserInfoResponse;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.DeviceType;
import com.semosan.api.domain.user.enums.OAuthProvider;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 카카오 유저 조회/등록/복구
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

    // 애플 유저 조회/등록/복구
    @Transactional
    public User findOrRegisterAppleUser(String appleId, String email, String name, DeviceType deviceType) {
        return userRepository.findByOauthIdAndOauthProvider(appleId, OAuthProvider.APPLE)
                .map(user -> {
                    if (user.isDeleted())
                        user.restore(email, name, null, deviceType);
                    return user;
                })
                .orElseGet(() ->
                        userRepository.save(User.createAppleUser(appleId, email, name, deviceType))
                );
    }


    @Transactional(readOnly = true)
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }

    // 테스트 유저 조회 or 생성
    @Transactional
    public User findOrCreateTestUser(String testUserId, DeviceType deviceType) {
        return userRepository.findByOauthIdAndOauthProvider(testUserId, OAuthProvider.KAKAO)
                .orElseGet(() -> userRepository.save(User.createTestUser(testUserId, deviceType)));
    }

}