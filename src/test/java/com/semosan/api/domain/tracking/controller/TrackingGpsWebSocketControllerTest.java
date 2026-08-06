package com.semosan.api.domain.tracking.controller;

import com.semosan.api.domain.tracking.dto.message.GpsPointMessage;
import com.semosan.api.domain.tracking.service.TrackingGpsPublisher;
import com.semosan.api.domain.tracking.websocket.UserIdPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrackingGpsWebSocketControllerTest {

    @Mock
    private TrackingGpsPublisher trackingGpsPublisher;

    @InjectMocks
    private TrackingGpsWebSocketController controller;

    @Test
    void receiveGpsPublishesWithUserIdFromPrincipal() {
        GpsPointMessage message = new GpsPointMessage(37.5, 127.0, 123.4, LocalDateTime.now());

        controller.receiveGps(100L, message, new UserIdPrincipal(1L));

        verify(trackingGpsPublisher).publish(1L, 100L, message);
    }

    @Test
    void receiveGpsThrowsWhenPrincipalIsNotUserIdPrincipal() {
        GpsPointMessage message = new GpsPointMessage(37.5, 127.0, null, LocalDateTime.now());
        Principal principal = () -> "anonymous";

        assertThatThrownBy(() -> controller.receiveGps(100L, message, principal))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unauthenticated WebSocket message");
    }
}
