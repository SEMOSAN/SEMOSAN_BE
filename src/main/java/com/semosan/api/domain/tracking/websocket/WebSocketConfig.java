package com.semosan.api.domain.tracking.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 트래킹 GPS 수집용 STOMP-over-WebSocket 설정.
 *  - 엔드포인트: /ws/tracking
 *  - 클라이언트 → 서버 메시지 prefix: /app  (예: /app/tracking/{sessionId}/gps)
 *  - 서버 → 클라이언트 토픽: /topic       (예: /topic/tracking/{sessionId}/stats, /notify — #46 에서 사용)
 *  - 인증: CONNECT 프레임 Authorization 헤더의 JWT 검증 (StompAuthChannelInterceptor)
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/tracking")
                .setAllowedOriginPatterns("http://localhost:*", "https://lgenius.site");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
