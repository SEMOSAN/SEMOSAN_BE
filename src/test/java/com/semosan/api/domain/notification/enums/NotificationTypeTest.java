package com.semosan.api.domain.notification.enums;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTypeTest {

    @Test
    void validatePassesWhenAllRequiredParamsExist() {
        NotificationType.COMMUNITY_COMMENT.validate(Map.of(
                "actorName", "푸름",
                "commentPreview", "댓글"
        ));
    }

    @Test
    void validateThrowsWhenParamsAreNull() {
        assertThatThrownBy(() -> NotificationType.COMMUNITY_COMMENT.validate(null))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.NOTIFICATION_PARAMS_INVALID);
    }

    @Test
    void validateThrowsWhenRequiredValueIsMissing() {
        assertThatThrownBy(() -> NotificationType.COMMUNITY_COMMENT.validate(Map.of("actorName", "푸름")))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.NOTIFICATION_PARAMS_INVALID);
    }

    @Test
    void validateThrowsWhenRequiredStringIsBlank() {
        assertThatThrownBy(() -> NotificationType.COMMUNITY_COMMENT.validate(Map.of(
                "actorName", " ",
                "commentPreview", "댓글"
        )))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.NOTIFICATION_PARAMS_INVALID);
    }

    @Test
    void validatePassesWhenTypeHasNoRequiredKeysAndParamsAreEmpty() {
        NotificationType.TRACKING_SUMMIT_REACHED.validate(Map.of());
    }

    @Test
    void formatTitleReturnsTemplateWhenParamsAreNullOrEmpty() {
        assertThat(NotificationType.COMMUNITY_COMMENT.formatTitle(null)).isEqualTo("새 댓글이 달렸어요");
        assertThat(NotificationType.COMMUNITY_COMMENT.formatBody(Map.of())).isEqualTo("{actorName}: {commentPreview}");
    }

    @Test
    void formatBodyReplacesProvidedParamsAndKeepsMissingPlaceholders() {
        String body = NotificationType.COMMUNITY_COMMENT.formatBody(Map.of("actorName", "푸름"));

        assertThat(body).isEqualTo("푸름: {commentPreview}");
    }

    @Test
    void formatBodyConvertsNonStringValues() {
        String body = NotificationType.TRACKING_PHOTO_MILESTONE.formatBody(Map.of("distance", 500));

        assertThat(body).isEqualTo("500m 돌파! 인증 사진을 남겨보세요!");
    }

    @Test
    void allCurrentNotificationTypesAreNotDataOnly() {
        assertThat(NotificationType.COMMUNITY_COMMENT.isDataOnly()).isFalse();
        assertThat(NotificationType.TRACKING_SUMMIT_REACHED.isDataOnly()).isFalse();
    }
}
