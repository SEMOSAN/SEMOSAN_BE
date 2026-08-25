package com.semosan.api.domain.notification.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.notification.entity.FcmToken;
import com.semosan.api.domain.notification.repository.FcmTokenRepository;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.service.UserReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcmTokenServiceTest {

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private UserReader userReader;

    @InjectMocks
    private FcmTokenService fcmTokenService;

    @Test
    void registerSavesNewTokenWhenTokenDoesNotExist() {
        when(fcmTokenRepository.findByToken("token")).thenReturn(Optional.empty());
        ArgumentCaptor<FcmToken> captor = ArgumentCaptor.forClass(FcmToken.class);

        fcmTokenService.register(1L, "token", DeviceType.IOS);

        verify(userReader).findActiveUserById(1L);
        verify(fcmTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getToken()).isEqualTo("token");
        assertThat(captor.getValue().getDeviceType()).isEqualTo(DeviceType.IOS);
    }

    @Test
    void registerReassignsExistingToken() {
        FcmToken token = FcmToken.create(1L, "token", DeviceType.IOS);
        when(fcmTokenRepository.findByToken("token")).thenReturn(Optional.of(token));

        fcmTokenService.register(2L, "token", DeviceType.ANDROID);

        assertThat(token.getUserId()).isEqualTo(2L);
        assertThat(token.getDeviceType()).isEqualTo(DeviceType.ANDROID);
        verify(fcmTokenRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void registerDoesNotThrowWhenConcurrentDuplicateDetected() {
        when(fcmTokenRepository.findByToken("token")).thenReturn(Optional.empty());
        when(fcmTokenRepository.save(any(FcmToken.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatCode(() -> fcmTokenService.register(1L, "token", DeviceType.IOS))
                .doesNotThrowAnyException();

        verify(userReader).findActiveUserById(1L);
        verify(fcmTokenRepository).save(any(FcmToken.class));
    }

    @Test
    void deleteDeletesOnlyOwnerToken() {
        FcmToken token = FcmToken.create(1L, "token", DeviceType.IOS);
        when(fcmTokenRepository.findByToken("token")).thenReturn(Optional.of(token));

        fcmTokenService.delete(1L, "token");

        verify(userReader).findActiveUserById(1L);
        verify(fcmTokenRepository).deleteByToken("token");
    }

    @Test
    void deleteThrowsWhenTokenBelongsToOtherUser() {
        FcmToken token = FcmToken.create(2L, "token", DeviceType.IOS);
        when(fcmTokenRepository.findByToken("token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> fcmTokenService.delete(1L, "token"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.FORBIDDEN);
        verify(fcmTokenRepository, never()).deleteByToken("token");
    }

    @Test
    void deleteDoesNothingWhenTokenDoesNotExist() {
        when(fcmTokenRepository.findByToken("token")).thenReturn(Optional.empty());

        fcmTokenService.delete(1L, "token");

        verify(fcmTokenRepository, never()).deleteByToken("token");
    }

    @Test
    void deleteExpiredDeletesWithoutUserValidation() {
        fcmTokenService.deleteExpired("token");

        verify(fcmTokenRepository).deleteByToken("token");
        verify(userReader, never()).findActiveUserById(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void deleteAllByUserIdDeletesAllUserTokens() {
        fcmTokenService.deleteAllByUserId(1L);

        verify(fcmTokenRepository).deleteAllByUserId(1L);
    }
}
