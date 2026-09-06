package com.semosan.api.domain.tracking.websocket;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.jwt.JwtService;
import com.semosan.api.common.jwt.TokenType;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.tracking.repository.TrackingSessionRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STOMP CONNECT 단계에서 Authorization 헤더의 JWT 를 검증하고,
 * 추출한 userId 를 UserIdPrincipal 로 세션에 바인딩한다.
 * 이후 같은 WebSocket 세션의 모든 메시지는 인증된 상태로 처리된다.
 * CONNECT 시 토큰 타입(ACCESS)과 유저 상태(탈퇴/정지)를 JwtFilter 와 동일하게 검증한다.
 * SUBSCRIBE 단계에서는 목적지 세션의 소유자인지 추가로 인가한다.
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

    // 구독 허용 토픽: /topic/tracking/{sessionId}/{suffix} — 그 외 목적지는 기본 거부
    private static final Pattern TRACKING_TOPIC_PATTERN =
            Pattern.compile("^/topic/tracking/(\\d+)/[\\w-]+$");

    private final JwtService jwtService;
    private final TrackingSessionRepository trackingSessionRepository;
    private final UserRepository userRepository;

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
        if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    /**
     * SUBSCRIBE 인가 — 세션 소유자만 자기 세션 토픽을 구독할 수 있다.
     * sessionId 가 순차 증가 값이라 타인 세션 토픽을 열거해 위치 진행 상황을
     * 엿볼 수 있으므로, CONNECT 인증만으로는 부족하고 목적지 단위 검증이 필요하다.
     */
    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();
        Principal user = accessor.getUser();
        if (!(user instanceof UserIdPrincipal principal)) {
            log.warn("STOMP SUBSCRIBE 거부: 미인증 세션. sessionId={} dest={}", sessionId, destination);
            throw new GeneralException(ErrorStatus.JWT_TOKEN_NOT_FOUND);
        }
        Long trackingSessionId = parseTrackingSessionId(destination);
        if (trackingSessionId == null) {
            log.warn("STOMP SUBSCRIBE 거부: 허용되지 않은 목적지. sessionId={} userId={} dest={}",
                    sessionId, principal.getUserId(), destination);
            throw new GeneralException(ErrorStatus.FORBIDDEN);
        }
        if (!trackingSessionRepository.existsByIdAndUser_Id(trackingSessionId, principal.getUserId())) {
            log.warn("STOMP SUBSCRIBE 거부: 세션 소유자 아님. sessionId={} userId={} trackingSessionId={}",
                    sessionId, principal.getUserId(), trackingSessionId);
            throw new GeneralException(ErrorStatus.TRACKING_SESSION_FORBIDDEN);
        }
    }

    private static Long parseTrackingSessionId(String destination) {
        if (destination == null) {
            return null;
        }
        Matcher matcher = TRACKING_TOPIC_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        String authHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("STOMP CONNECT 인증 실패: Authorization 헤더 누락/잘못된 형식. sessionId={}", sessionId);
            throw new GeneralException(ErrorStatus.JWT_TOKEN_NOT_FOUND);
        }
        String token = authHeader.substring(BEARER_PREFIX.length());
        Claims claims;
        try {
            claims = jwtService.validateAccessTokenAndGetClaims(token);
        } catch (GeneralException e) {
            log.warn("STOMP CONNECT 인증 실패: 토큰 검증 실패. sessionId={} code={} msg={}",
                    sessionId, e.getErrorStatus().getCode(), e.getMessage());
            throw e;
        }
        if (jwtService.isAccessTokenBlacklisted(token)) {
            log.warn("STOMP CONNECT 인증 실패: 블랙리스트 토큰. sessionId={}", sessionId);
            throw new GeneralException(ErrorStatus.JWT_BLACKLISTED);
        }
        // ADMIN 토큰은 subject 가 admin.id 라 그대로 바인딩하면 같은 숫자 id 의 유저로 행세하게 된다.
        // 트래킹 WS 는 관리자 표면이 아니므로 ACCESS 토큰만 허용한다.
        if (TokenType.ACCESS != jwtService.getTokenType(claims)) {
            log.warn("STOMP CONNECT 인증 실패: ACCESS 토큰이 아님. sessionId={}", sessionId);
            throw new GeneralException(ErrorStatus.JWT_INVALID_TYPE);
        }
        Long userId;
        try {
            userId = jwtService.getUserIdFromClaims(claims);
        } catch (GeneralException e) {
            log.warn("STOMP CONNECT 인증 실패: userId 추출 실패. sessionId={} code={} msg={}",
                    sessionId, e.getErrorStatus().getCode(), e.getMessage());
            throw e;
        }
        verifyUserIsActive(sessionId, userId);
        accessor.setUser(new UserIdPrincipal(userId));
        log.info("STOMP CONNECT 인증 성공: sessionId={} userId={}", sessionId, userId);
    }

    /**
     * JwtFilter 의 HTTP 인증과 동일한 유저 상태 검증.
     * 이 검증이 없으면 정지·탈퇴 유저가 토큰 만료 전까지 WS 로만 계속 접속할 수 있다.
     */
    private void verifyUserIsActive(String sessionId, Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> {
                    log.warn("STOMP CONNECT 인증 실패: 탈퇴/미존재 유저. sessionId={} userId={}", sessionId, userId);
                    return new GeneralException(ErrorStatus.JWT_USER_WITHDRAWN);
                });
        if (user.isSuspended()) {
            log.warn("STOMP CONNECT 인증 실패: 정지 유저. sessionId={} userId={}", sessionId, userId);
            throw new GeneralException(ErrorStatus.USER_SUSPENDED);
        }
    }
}
