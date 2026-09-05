package com.semosan.api.domain.notification.enums;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
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

    @Test
    void targetTypeMapsEachNotificationToItsDestinationScreen() {
        assertThat(NotificationType.SEMOFEED_EMOJI.getTargetType()).isEqualTo(NotificationTargetType.SEMOFEED);
        assertThat(NotificationType.COMMUNITY_COMMENT.getTargetType()).isEqualTo(NotificationTargetType.COMMUNITY_POST);
        assertThat(NotificationType.COMMUNITY_REPLY.getTargetType()).isEqualTo(NotificationTargetType.COMMUNITY_POST);
        assertThat(NotificationType.COMMUNITY_POST_LIKE.getTargetType()).isEqualTo(NotificationTargetType.COMMUNITY_POST);
        assertThat(NotificationType.TRACKING_PHOTO_MILESTONE.getTargetType()).isEqualTo(NotificationTargetType.NONE);
        assertThat(NotificationType.TRACKING_SUMMIT_REACHED.getTargetType()).isEqualTo(NotificationTargetType.NONE);
    }

    @Test
    void resolveTargetIdReadsSemoFeedIdFromExtras() {
        Long targetId = NotificationType.SEMOFEED_EMOJI.resolveTargetId(Map.of(
                "actorId", 1L,
                "actorName", "푸름",
                "semoFeedId", 42L,
                "emojiType", "🔥"
        ));

        assertThat(targetId).isEqualTo(42L);
    }

    @Test
    void resolveTargetIdReadsPostIdFromExtras() {
        assertThat(NotificationType.COMMUNITY_COMMENT.resolveTargetId(Map.of("postId", 7L))).isEqualTo(7L);
        assertThat(NotificationType.COMMUNITY_REPLY.resolveTargetId(Map.of("postId", 7L))).isEqualTo(7L);
        assertThat(NotificationType.COMMUNITY_POST_LIKE.resolveTargetId(Map.of("postId", 7L))).isEqualTo(7L);
    }

    @Test
    void resolveTargetIdAcceptsIntegerAndStringBecauseJsonbRoundTripChangesTheType() {
        assertThat(NotificationType.SEMOFEED_EMOJI.resolveTargetId(Map.of("semoFeedId", 42))).isEqualTo(42L);
        assertThat(NotificationType.SEMOFEED_EMOJI.resolveTargetId(Map.of("semoFeedId", "42"))).isEqualTo(42L);
        assertThat(NotificationType.SEMOFEED_EMOJI.resolveTargetId(Map.of("semoFeedId", " 42 "))).isEqualTo(42L);
    }

    @Test
    void resolveTargetIdReturnsNullWhenValueCannotBeReadAsId() {
        assertThat(NotificationType.SEMOFEED_EMOJI.resolveTargetId(null)).isNull();
        assertThat(NotificationType.SEMOFEED_EMOJI.resolveTargetId(Map.of())).isNull();
        assertThat(NotificationType.SEMOFEED_EMOJI.resolveTargetId(Map.of("semoFeedId", "not-a-number"))).isNull();
        assertThat(NotificationType.SEMOFEED_EMOJI.resolveTargetId(Map.of("semoFeedId", List.of(1)))).isNull();
    }

    @Test
    void resolveTargetIdReturnsNullForTypesWithoutDestination() {
        assertThat(NotificationType.TRACKING_PHOTO_MILESTONE.resolveTargetId(Map.of("distance", 500))).isNull();
        assertThat(NotificationType.TRACKING_SUMMIT_REACHED.resolveTargetId(Map.of("milestoneIndex", 3))).isNull();
    }
}
