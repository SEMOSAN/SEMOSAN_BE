package com.semosan.api.domain.notification.service;

import com.semosan.api.domain.notification.entity.FcmToken;
import com.semosan.api.domain.notification.repository.FcmTokenRepository;
import com.semosan.api.domain.user.enums.DeviceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;

    /**
     * 토큰 등록
     * 같은 토큰이 이미 있으면 (다른 유저로 로그인 등) 소유자/디바이스 갱신
     */
    @Transactional
    public void register(Long userId, String token, DeviceType deviceType) {
        fcmTokenRepository.findByToken(token).ifPresentOrElse(
                existing -> existing.reassignTo(userId, deviceType),
                () -> fcmTokenRepository.save(FcmToken.create(userId, token, deviceType))
        );
    }

    /**
     * 토큰 삭제
     * 로그아웃 시 호출하면 될 듯
     */
    @Transactional
    public void delete(String token) {
        fcmTokenRepository.deleteByToken(token);
    }
}
