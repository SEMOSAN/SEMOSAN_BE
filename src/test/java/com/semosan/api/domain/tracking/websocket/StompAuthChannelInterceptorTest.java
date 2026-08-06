package com.semosan.api.domain.tracking.websocket;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.common.status.ErrorStatus;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private StompAuthChannelInterceptor interceptor;

    @Test
    void preSendAuthenticatesConnectMessageAndSetsUserPrincipal() {
        Claims claims = mock(Claims.class);
        Message<?> message = connectMessage("Bearer access-token");
        when(jwtService.validateAccessTokenAndGetClaims("access-token")).thenReturn(claims);
        when(jwtService.isAccessTokenBlacklisted("access-token")).thenReturn(false);
        when(jwtService.getUserIdFromClaims(claims)).thenReturn(1L);

        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        StompHeaderAccessor resultAccessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        Principal user = resultAccessor.getUser();
        assertThat(user).isInstanceOf(UserIdPrincipal.class);
        assertThat(user.getName()).isEqualTo("1");
        assertThat(((UserIdPrincipal) user).getUserId()).isEqualTo(1L);
    }

    @Test
    void preSendReturnsMessageWithoutAuthenticationForNonConnectCommand() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        assertThat(result).isSameAs(message);
        verify(jwtService, never()).validateAccessTokenAndGetClaims(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void preSendThrowsWhenAuthorizationHeaderMissing() {
        Message<?> message = connectMessage(null);

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.JWT_TOKEN_NOT_FOUND);
    }

    @Test
    void preSendThrowsWhenTokenBlacklisted() {
        Claims claims = mock(Claims.class);
        Message<?> message = connectMessage("Bearer access-token");
        when(jwtService.validateAccessTokenAndGetClaims("access-token")).thenReturn(claims);
        when(jwtService.isAccessTokenBlacklisted("access-token")).thenReturn(true);

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.JWT_BLACKLISTED);
    }

    private Message<?> connectMessage(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId("session-1");
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
