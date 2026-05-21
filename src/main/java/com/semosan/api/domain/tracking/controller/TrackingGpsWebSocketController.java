package com.semosan.api.domain.tracking.controller;

import com.semosan.api.domain.tracking.dto.message.GpsPointMessage;
import com.semosan.api.domain.tracking.service.TrackingGpsPublisher;
import com.semosan.api.domain.tracking.websocket.UserIdPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * 클라이언트 STOMP SEND: /app/tracking/{sessionId}/gps
 * 인증된 사용자만 도달 (CONNECT 시 JWT 검증 완료). userId 는 Principal 에서 추출.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class TrackingGpsWebSocketController {

    private final TrackingGpsPublisher trackingGpsPublisher;

    @MessageMapping("/tracking/{sessionId}/gps")
    public void receiveGps(
            @DestinationVariable Long sessionId,
            @Valid @Payload GpsPointMessage message,
            Principal principal
    ) {
        Long userId = resolveUserId(principal);
        trackingGpsPublisher.publish(userId, sessionId, message);
    }

    private Long resolveUserId(Principal principal) {
        if (principal instanceof UserIdPrincipal userPrincipal) {
            return userPrincipal.getUserId();
        }
        throw new IllegalStateException("Unauthenticated WebSocket message");
    }
}
