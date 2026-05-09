package com.semosan.api.domain.user.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
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

    // 카카오 유저 조회 후 없으면 신규 생성, 탈퇴 유저면 복구
    @Transactional
    public User findOrRegisterKakaoUser(
            String kakaoId, String email, String name, String profileUrl, DeviceType deviceType
    ) {
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

    // 애플 유저 조회 후 없으면 신규 생성, 탈퇴 유저면 복구
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

    // userId로 삭제되지 않은 유저를 조회하고, 없으면 예외를 발생시킵니다.
    @Transactional(readOnly = true)
    public User findActiveUserById(Long userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }

    // 테스트 유저 조회 후 없으면 신규 생성
    @Transactional
    public User findOrCreateTestUser(String testUserId, DeviceType deviceType) {
        return userRepository.findByOauthIdAndOauthProvider(testUserId, OAuthProvider.TEST)
                .orElseGet(() -> userRepository.save(User.createTestUser(testUserId, deviceType)));
    }

}
