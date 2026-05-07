package com.semosan.api.domain.notification.enums;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;

import java.util.Map;
import java.util.Set;

public enum NotificationType {

    // 테스트 용
    COMMUNITY_COMMENT(
            "새 댓글이 달렸어요",
            "{actorName}: {commentPreview}",
            Set.of("actorName", "commentPreview")
    );

    private final String titleTemplate;
    private final String bodyTemplate;
    private final Set<String> requiredKeys;

    NotificationType(String titleTemplate, String bodyTemplate, Set<String> requiredKeys) {
        this.titleTemplate = titleTemplate;
        this.bodyTemplate = bodyTemplate;
        this.requiredKeys = requiredKeys;
    }

    /**
     * 호출자가 필수 파라미터를 누락했을 때 예외. 알림 발송 전 미리 차단.
     */
    public void validate(Map<String, Object> params) {
        if (params == null) {
            throw new GeneralException(ErrorStatus.NOTIFICATION_PARAMS_INVALID);
        }
        for (String key : requiredKeys) {
            Object value = params.get(key);
            if (value == null || (value instanceof String s && s.isBlank())) {
                throw new GeneralException(ErrorStatus.NOTIFICATION_PARAMS_INVALID);
            }
        }
    }

    public String formatTitle(Map<String, Object> params) {
        return format(titleTemplate, params);
    }

    public String formatBody(Map<String, Object> params) {
        return format(bodyTemplate, params);
    }

    private static String format(String template, Map<String, Object> params) {
        if (params == null || params.isEmpty()) return template;
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }
}
