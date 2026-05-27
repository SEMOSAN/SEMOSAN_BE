package com.semosan.api.domain.notification.dispatcher;

import com.semosan.api.domain.notification.enums.NotificationType;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class FcmPayloadPolicy {

    private static final Set<NotificationType> DATA_ONLY_TYPES = Set.of(
            NotificationType.TRACKING_PHOTO_MILESTONE,
            NotificationType.TRACKING_SUMMIT_REACHED
    );

    public boolean isDataOnly(NotificationType type) {
        return DATA_ONLY_TYPES.contains(type);
    }
}
