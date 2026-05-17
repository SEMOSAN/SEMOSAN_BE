package com.semosan.api.domain.tracking.websocket;

import java.security.Principal;

/**
 * STOMP CONNECT 핸드셰이크 시 JWT 에서 추출한 userId 를 들고 다니기 위한 Principal.
 * MessageMapping 핸들러에서 Principal 인자로 받아 userId 를 꺼낸다.
 */
public class UserIdPrincipal implements Principal {

    private final Long userId;

    public UserIdPrincipal(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
