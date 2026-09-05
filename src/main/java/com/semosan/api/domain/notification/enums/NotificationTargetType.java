package com.semosan.api.domain.notification.enums;

/**
 * 클라이언트는 이 값만 보고 라우팅한다 — 알림 타입이 늘어도 앱 수정 없이 서버에서 흡수하기 위한 계약.
 */
public enum NotificationTargetType {
    NONE,
    SEMOFEED,
    COMMUNITY_POST
}
