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
    void validatePassesWhenSummitReachedCarriesMilestoneParams() {
        NotificationType.TRACKING_SUMMIT_REACHED.validate(Map.of(
                "milestoneIndex", 3,
                "milestoneDistanceM", 2000.0
        ));
    }

    @Test
    void validateThrowsWhenSummitReachedIsMissingMilestoneParams() {
        // 정상 인증 사진 업로드에 필요한 값이라 누락되면 발송 전에 막아야 한다.
        assertThatThrownBy(() -> NotificationType.TRACKING_SUMMIT_REACHED.validate(Map.of()))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.NOTIFICATION_PARAMS_INVALID);
    }

    @Test
    void validateThrowsWhenPhotoMilestoneIsMissingMilestoneIndex() {
        assertThatThrownBy(() -> NotificationType.TRACKING_PHOTO_MILESTONE.validate(Map.of("distance", 500)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.NOTIFICATION_PARAMS_INVALID);
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
