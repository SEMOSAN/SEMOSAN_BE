package com.semosan.api.domain.notification.enums;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;

import java.util.Map;
import java.util.Set;

public enum NotificationType {

    COMMUNITY_COMMENT(
            "새 댓글이 달렸어요",
            "{actorName}: {commentPreview}",
            Set.of("actorName", "commentPreview"),
            false
    ),

    COMMUNITY_REPLY(
            "새 답글이 달렸어요",
            "{actorName}: {commentPreview}",
            Set.of("actorName", "commentPreview"),
            false
    ),

    COMMUNITY_POST_LIKE(
            "게시글에 좋아요가 눌렸어요",
            "{actorName}님이 게시글을 좋아합니다",
            Set.of("actorName"),
            false
    ),

    SEMOFEED_EMOJI(
            "세모피드에 반응이 달렸어요",
            "{actorName}님이 세모피드에 {emojiType} 반응을 남겼어요",
            Set.of("actorId", "actorName", "semoFeedId", "emojiType"),
            false
    ),

    /**
     * 트래킹 중 거리 마일스톤 도달 시 사진 촬영 유도.
     * iOS 백그라운드/잠금화면/앱 종료 상태에서도 시스템이 즉시 배너를 표시하도록 mixed payload
     * (notification 키 + data 키) 로 발송한다. 포그라운드 자동 배너 차단은 클라(앱) 의
     * setForegroundNotificationPresentationOptions 에서 처리한다.
     */
    TRACKING_PHOTO_MILESTONE(
            "SEMOSAN",
            "{distance}m 돌파! 인증 사진을 남겨보세요!",
            Set.of("distance"),
            false
    ),

    /**
     * 코스 거리 50% 도달 시 정상 인증 유도.
     * 진짜 정상 좌표가 식별 불가해 코스 절반 지점을 "정상" 근처로 간주하는 임시 정책.
     * iOS 백그라운드/잠금화면/앱 종료 상태에서도 시스템이 즉시 배너를 표시하도록 mixed payload
     * (notification 키 + data 키) 로 발송한다.
     */
    TRACKING_SUMMIT_REACHED(
            "SEMOSAN",
            "정상에 도착했나요? 정상 인증하기!",
            Set.of(),
            false
    );

    private final String titleTemplate;
    private final String bodyTemplate;
    private final Set<String> requiredKeys;
    private final boolean dataOnly;

    NotificationType(String titleTemplate, String bodyTemplate, Set<String> requiredKeys, boolean dataOnly) {
        this.titleTemplate = titleTemplate;
        this.bodyTemplate = bodyTemplate;
        this.requiredKeys = requiredKeys;
        this.dataOnly = dataOnly;
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

    public boolean isDataOnly() {
        return dataOnly;
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
