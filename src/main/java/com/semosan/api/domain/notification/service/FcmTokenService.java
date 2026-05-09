package com.semosan.api.domain.notification.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.notification.entity.FcmToken;
import com.semosan.api.domain.notification.repository.FcmTokenRepository;
import com.semosan.api.domain.user.enums.user.DeviceType;
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
     * 토큰 삭제 (로그아웃 시 호출)
     * 본인 소유 토큰만 삭제 가능. 토큰 없으면 idempotent하게 noop.
     */
    @Transactional
    public void delete(Long userId, String token) {
        fcmTokenRepository.findByToken(token).ifPresent(fcmToken -> {
            if (!fcmToken.getUserId().equals(userId)) {
                throw new GeneralException(ErrorStatus.FORBIDDEN);
            }
            fcmTokenRepository.deleteByToken(token);
        });
    }

    /**
     * FCM 발송 실패(UNREGISTERED 등)로 만료된 토큰 정리.
     * 시스템 정리용이라 본인 검증 없이 바로 삭제.
     */
    @Transactional
    public void deleteExpired(String token) {
        fcmTokenRepository.deleteByToken(token);
    }
}
