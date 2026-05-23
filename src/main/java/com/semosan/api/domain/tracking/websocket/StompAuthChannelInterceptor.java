package com.semosan.api.domain.tracking.websocket;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.common.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * STOMP CONNECT 단계에서 Authorization 헤더의 JWT 를 검증하고,
 * 추출한 userId 를 UserIdPrincipal 로 세션에 바인딩한다.
 * 이후 같은 WebSocket 세션의 모든 메시지는 인증된 상태로 처리된다.
 *
 * 진단 로그: CONNECT 진입/성공/실패, 그 외 STOMP command 종류를 모두 기록.
 * STOMP 채널은 GeneralExceptionAdvice 가 안 잡아서 기본 에러가 서버 로그에 안 남으므로,
 * 여기서 catch + log 로 잡지 않으면 클라이언트의 STOMP ERROR 프레임에만 의존하게 됨.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (command != null) {
            log.debug("STOMP {} sessionId={} dest={}",
                    command, accessor.getSessionId(), accessor.getDestination());
        }
        if (StompCommand.CONNECT.equals(command)) {
            authenticate(accessor);
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        String authHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("STOMP CONNECT 인증 실패: Authorization 헤더 누락/잘못된 형식. sessionId={}", sessionId);
            throw new GeneralException(ErrorStatus.JWT_TOKEN_NOT_FOUND);
        }
        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            jwtService.validateAccessToken(token);
        } catch (GeneralException e) {
            log.warn("STOMP CONNECT 인증 실패: 토큰 검증 실패. sessionId={} code={} msg={}",
                    sessionId, e.getErrorStatus().getCode(), e.getMessage());
            throw e;
        }
        if (jwtService.isAccessTokenBlacklisted(token)) {
            log.warn("STOMP CONNECT 인증 실패: 블랙리스트 토큰. sessionId={}", sessionId);
            throw new GeneralException(ErrorStatus.JWT_BLACKLISTED);
        }
        Long userId;
        try {
            userId = jwtService.getUserIdFromJwtToken(token);
        } catch (GeneralException e) {
            log.warn("STOMP CONNECT 인증 실패: userId 추출 실패. sessionId={} code={} msg={}",
                    sessionId, e.getErrorStatus().getCode(), e.getMessage());
            throw e;
        }
        accessor.setUser(new UserIdPrincipal(userId));
        log.info("STOMP CONNECT 인증 성공: sessionId={} userId={}", sessionId, userId);
    }
}
