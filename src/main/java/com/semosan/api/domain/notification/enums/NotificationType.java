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
            false,
            NotificationTargetType.COMMUNITY_POST,
            "postId"
    ),

    COMMUNITY_REPLY(
            "새 답글이 달렸어요",
            "{actorName}: {commentPreview}",
            Set.of("actorName", "commentPreview"),
            false,
            NotificationTargetType.COMMUNITY_POST,
            "postId"
    ),

    COMMUNITY_POST_LIKE(
            "게시글에 좋아요가 눌렸어요",
            "{actorName}님이 게시글을 좋아합니다",
            Set.of("actorName"),
            false,
            NotificationTargetType.COMMUNITY_POST,
            "postId"
    ),

    SEMOFEED_EMOJI(
            "세모피드에 반응이 달렸어요",
            "{actorName}님이 세모피드에 {emojiType} 반응을 남겼어요",
            Set.of("actorId", "actorName", "semoFeedId", "emojiType"),
            false,
            NotificationTargetType.SEMOFEED,
            "semoFeedId"
    ),

    /**
     * 트래킹 중 거리 마일스톤 도달 시 사진 촬영 유도.
     * data 에 distance(m), milestoneIndex 를 싣는다 — 클라이언트가 사진 업로드 API 에 그대로 넘긴다.
     * iOS 백그라운드/잠금화면/앱 종료 상태에서도 시스템이 즉시 배너를 표시하도록 mixed payload
     * (notification 키 + data 키) 로 발송한다. 포그라운드 배너 노출 여부는 클라(앱) 의
     * UNUserNotificationCenterDelegate(willPresent) 에서 알림 타입(data.type) 을 식별해
     * 동적으로 제어한다. (setForegroundNotificationPresentationOptions 는 앱 전역 옵션이라
     * 다른 알림 타입에도 영향이 가므로 사용하지 않는다.)
     */
    TRACKING_PHOTO_MILESTONE(
            "SEMOSAN",
            "{distance}m 돌파! 인증 사진을 남겨보세요!",
            Set.of("distance", "milestoneIndex"),
            false,
            NotificationTargetType.NONE,
            null
    ),

    /**
     * 시작점→정상 누적 거리 도달 시 정상 인증 유도.
     * data 에 milestoneIndex, milestoneDistanceM 을 싣는다 — 정상 인증 사진 업로드에 필요한 값이다.
     * 정상과 일치하는 마일스톤 인덱스는 코스마다 다르다(정상 좌표 있으면 3, fallback 이면 1).
     * 정상 좌표(courses.summit_lat/lng)가 없는 코스만 코스 절반 지점을 "정상" 근처로 간주한다.
     * iOS 백그라운드/잠금화면/앱 종료 상태에서도 시스템이 즉시 배너를 표시하도록 mixed payload
     * (notification 키 + data 키) 로 발송한다. 포그라운드 배너 제어는 TRACKING_PHOTO_MILESTONE
     * 과 동일하게 UNUserNotificationCenterDelegate(willPresent) 에서 동적으로 처리한다.
     */
    TRACKING_SUMMIT_REACHED(
            "SEMOSAN",
            "정상에 도착했나요? 정상 인증하기!",
            Set.of("milestoneIndex", "milestoneDistanceM"),
            false,
            NotificationTargetType.NONE,
            null
    );

    private final String titleTemplate;
    private final String bodyTemplate;
    private final Set<String> requiredKeys;
    private final boolean dataOnly;
    private final NotificationTargetType targetType;
    private final String targetKey;

    NotificationType(
            String titleTemplate,
            String bodyTemplate,
            Set<String> requiredKeys,
            boolean dataOnly,
            NotificationTargetType targetType,
            String targetKey
    ) {
        this.titleTemplate = titleTemplate;
        this.bodyTemplate = bodyTemplate;
        this.requiredKeys = requiredKeys;
        this.dataOnly = dataOnly;
        this.targetType = targetType;
        this.targetKey = targetKey;
    }

    public NotificationTargetType getTargetType() {
        return targetType;
    }

    /**
     * extras 는 jsonb 로 왕복하면서 Integer/Long/String 중 무엇으로든 돌아올 수 있어 세 경우를 모두 받는다.
     * 해석 불가 시 null — 호출부에서 이동 불가 알림으로 처리한다.
     */
    public Long resolveTargetId(Map<String, Object> extras) {
        if (targetKey == null || extras == null) {
            return null;
        }
        Object value = extras.get(targetKey);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
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
