package com.semosan.api.domain.tracking.websocket;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.common.jwt.TokenType;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.tracking.repository.TrackingSessionRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.repository.UserRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

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

    @Mock
    private TrackingSessionRepository trackingSessionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StompAuthChannelInterceptor interceptor;

    @Test
    void preSendAuthenticatesConnectMessageAndSetsUserPrincipal() {
        Claims claims = mock(Claims.class);
        Message<?> message = connectMessage("Bearer access-token");
        when(jwtService.validateAccessTokenAndGetClaims("access-token")).thenReturn(claims);
        when(jwtService.isAccessTokenBlacklisted("access-token")).thenReturn(false);
        when(jwtService.getTokenType(claims)).thenReturn(TokenType.ACCESS);
        when(jwtService.getUserIdFromClaims(claims)).thenReturn(1L);
        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user(1L)));

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
    void preSendReturnsMessageWhenStompAccessorIsMissing() {
        Message<?> message = MessageBuilder.withPayload(new byte[0]).build();

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
    void preSendThrowsWhenAuthorizationHeaderDoesNotStartWithBearer() {
        Message<?> message = connectMessage("Basic access-token");

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

    @Test
    void preSendAllowsSubscribeWhenUserOwnsTrackingSession() {
        Message<?> message = subscribeMessage("/topic/tracking/46/summit", new UserIdPrincipal(1L));
        when(trackingSessionRepository.existsByIdAndUser_Id(46L, 1L)).thenReturn(true);

        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        assertThat(result).isSameAs(message);
    }

    @Test
    void preSendThrowsWhenSubscribingToOtherUsersTrackingSession() {
        Message<?> message = subscribeMessage("/topic/tracking/46/photo-window", new UserIdPrincipal(2L));
        when(trackingSessionRepository.existsByIdAndUser_Id(46L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_SESSION_FORBIDDEN);
    }

    @Test
    void preSendThrowsWhenSubscribeHasNoAuthenticatedUser() {
        Message<?> message = subscribeMessage("/topic/tracking/46/summit", null);

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.JWT_TOKEN_NOT_FOUND);
    }

    @Test
    void preSendThrowsWhenSubscribingToUnknownDestination() {
        Message<?> message = subscribeMessage("/topic/other/1", new UserIdPrincipal(1L));

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.FORBIDDEN);
    }

    @Test
    void preSendThrowsWhenSubscribeDestinationIsMissing() {
        Message<?> message = subscribeMessage(null, new UserIdPrincipal(1L));

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.FORBIDDEN);
    }

    @Test
    void preSendThrowsWhenConnectTokenIsNotAccessType() {
        Claims claims = mock(Claims.class);
        Message<?> message = connectMessage("Bearer admin-token");
        when(jwtService.validateAccessTokenAndGetClaims("admin-token")).thenReturn(claims);
        when(jwtService.isAccessTokenBlacklisted("admin-token")).thenReturn(false);
        when(jwtService.getTokenType(claims)).thenReturn(TokenType.ADMIN);

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.JWT_INVALID_TYPE);
        verify(userRepository, never()).findByIdAndDeletedFalse(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void preSendThrowsWhenConnectUserIsWithdrawn() {
        Claims claims = mock(Claims.class);
        Message<?> message = connectMessage("Bearer access-token");
        when(jwtService.validateAccessTokenAndGetClaims("access-token")).thenReturn(claims);
        when(jwtService.isAccessTokenBlacklisted("access-token")).thenReturn(false);
        when(jwtService.getTokenType(claims)).thenReturn(TokenType.ACCESS);
        when(jwtService.getUserIdFromClaims(claims)).thenReturn(1L);
        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.JWT_USER_WITHDRAWN);
    }

    @Test
    void preSendThrowsWhenConnectUserIsSuspended() {
        Claims claims = mock(Claims.class);
        Message<?> message = connectMessage("Bearer access-token");
        User suspended = user(1L);
        suspended.suspend(LocalDateTime.now().plusDays(1));
        when(jwtService.validateAccessTokenAndGetClaims("access-token")).thenReturn(claims);
        when(jwtService.isAccessTokenBlacklisted("access-token")).thenReturn(false);
        when(jwtService.getTokenType(claims)).thenReturn(TokenType.ACCESS);
        when(jwtService.getUserIdFromClaims(claims)).thenReturn(1L);
        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(suspended));

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.USER_SUSPENDED);
    }

    private User user(Long id) {
        User user = User.createTestUser("test-" + id, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Message<?> subscribeMessage(String destination, Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId("session-1");
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (user != null) {
            accessor.setUser(user);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
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
