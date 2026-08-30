package com.semosan.api.domain.user.event;

import com.semosan.api.common.alert.DiscordAlertClient;
import com.semosan.api.common.alert.dto.DiscordMessage;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.enums.user.OAuthProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserRegisteredEventListenerTest {

    @Mock
    private DiscordAlertClient discordAlertClient;

    @InjectMocks
    private UserRegisteredEventListener listener;

    @Test
    void onUserRegisteredSendsSignupMessage() {
        listener.onUserRegistered(new UserRegisteredEvent(
                42L,
                "용감한등산러1234",
                OAuthProvider.KAKAO,
                DeviceType.ANDROID,
                LocalDateTime.of(2026, 8, 30, 14, 22, 10)
        ));

        ArgumentCaptor<DiscordMessage> captor = forClass(DiscordMessage.class);
        verify(discordAlertClient).sendSignup(captor.capture());

        DiscordMessage message = captor.getValue();
        assertThat(message.content()).contains("신규 가입");
        assertThat(message.embeds()).hasSize(1);
        assertThat(message.embeds().getFirst().description())
                .contains("42")
                .contains("용감한등산러1234")
                .contains("KAKAO / ANDROID")
                .contains("2026-08-30 14:22:10");
    }

    @Test
    void onUserRegisteredRendersDashWhenRegisteredAtIsMissing() {
        listener.onUserRegistered(new UserRegisteredEvent(
                42L, "닉네임", OAuthProvider.APPLE, DeviceType.IOS, null
        ));

        ArgumentCaptor<DiscordMessage> captor = forClass(DiscordMessage.class);
        verify(discordAlertClient).sendSignup(captor.capture());

        assertThat(captor.getValue().embeds().getFirst().description())
                .contains("### 가입 시각\n-");
    }

    @Test
    void onUserRegisteredSwallowsAlertFailure() {
        doThrow(new RuntimeException("discord down"))
                .when(discordAlertClient).sendSignup(any(DiscordMessage.class));

        assertThatCode(() -> listener.onUserRegistered(new UserRegisteredEvent(
                42L, "닉네임", OAuthProvider.KAKAO, DeviceType.IOS, LocalDateTime.now()
        ))).doesNotThrowAnyException();
    }
}
