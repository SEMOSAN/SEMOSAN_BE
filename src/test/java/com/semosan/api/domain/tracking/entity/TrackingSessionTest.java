package com.semosan.api.domain.tracking.entity;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.tracking.enums.TrackingSessionStatus;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class TrackingSessionTest {

    @Test
    void createInitializesInProgressSession() {
        User user = user(1L);
        Mountain mountain = mock(Mountain.class);

        TrackingSession session = TrackingSession.create(user, mountain, null, true);

        assertThat(session.getUser()).isSameAs(user);
        assertThat(session.getMountain()).isSameAs(mountain);
        assertThat(session.getCourse()).isNull();
        assertThat(session.getIsFreeRecording()).isTrue();
        assertThat(session.getStatus()).isEqualTo(TrackingSessionStatus.IN_PROGRESS);
        assertThat(session.getStartedAt()).isNotNull();
        assertThat(session.getPausedSecondsTotal()).isZero();
    }

    @Test
    void pauseChangesStatusAndSetsPausedAt() {
        TrackingSession session = session();

        session.pause();

        assertThat(session.getStatus()).isEqualTo(TrackingSessionStatus.PAUSED);
        assertThat(session.getPausedAt()).isNotNull();
    }

    @Test
    void pauseThrowsWhenSessionIsNotInProgress() {
        TrackingSession session = session();
        session.pause();

        assertThatThrownBy(session::pause)
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_SESSION_INVALID_STATE);
    }

    @Test
    void resumeAccumulatesPausedSecondsAndClearsPausedAt() {
        TrackingSession session = session();
        session.pause();
        ReflectionTestUtils.setField(session, "pausedAt", LocalDateTime.now().minusSeconds(10));

        session.resume();

        assertThat(session.getStatus()).isEqualTo(TrackingSessionStatus.IN_PROGRESS);
        assertThat(session.getPausedAt()).isNull();
        assertThat(session.getPausedSecondsTotal()).isGreaterThanOrEqualTo(9);
    }

    @Test
    void resumeChangesStatusWhenPausedAtIsNull() {
        TrackingSession session = session();
        ReflectionTestUtils.setField(session, "status", TrackingSessionStatus.PAUSED);
        ReflectionTestUtils.setField(session, "pausedAt", null);
        ReflectionTestUtils.setField(session, "pausedSecondsTotal", 5);

        session.resume();

        assertThat(session.getStatus()).isEqualTo(TrackingSessionStatus.IN_PROGRESS);
        assertThat(session.getPausedSecondsTotal()).isEqualTo(5);
    }

    @Test
    void resumeThrowsWhenSessionIsNotPaused() {
        TrackingSession session = session();

        assertThatThrownBy(session::resume)
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_SESSION_INVALID_STATE);
    }

    @Test
    void completeFinalizesPausedSession() {
        TrackingSession session = session();
        session.pause();
        ReflectionTestUtils.setField(session, "pausedAt", LocalDateTime.now().minusSeconds(10));

        session.complete();

        assertThat(session.getStatus()).isEqualTo(TrackingSessionStatus.COMPLETED);
        assertThat(session.getEndedAt()).isNotNull();
        assertThat(session.getPausedAt()).isNull();
        assertThat(session.getPausedSecondsTotal()).isGreaterThanOrEqualTo(9);
    }

    @Test
    void completeThrowsWhenSessionAlreadyTerminal() {
        TrackingSession session = session();
        session.complete();

        assertThatThrownBy(session::complete)
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_SESSION_INVALID_STATE);
    }

    @Test
    void abandonFinalizesPausedSession() {
        TrackingSession session = session();
        session.pause();
        ReflectionTestUtils.setField(session, "pausedAt", LocalDateTime.now().minusSeconds(10));

        session.abandon();

        assertThat(session.getStatus()).isEqualTo(TrackingSessionStatus.ABANDONED);
        assertThat(session.getEndedAt()).isNotNull();
        assertThat(session.getPausedAt()).isNull();
        assertThat(session.getPausedSecondsTotal()).isGreaterThanOrEqualTo(9);
    }

    @Test
    void abandonThrowsWhenSessionAlreadyTerminal() {
        TrackingSession session = session();
        session.abandon();

        assertThatThrownBy(session::abandon)
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRACKING_SESSION_INVALID_STATE);
    }

    @Test
    void isOwnedByReturnsTrueOnlyForSameUserId() {
        TrackingSession session = session();

        assertThat(session.isOwnedBy(1L)).isTrue();
        assertThat(session.isOwnedBy(2L)).isFalse();
        assertThat(session.isOwnedBy(null)).isFalse();
    }

    @Test
    void isOwnedByReturnsFalseWhenUserIsNull() {
        TrackingSession session = session();
        ReflectionTestUtils.setField(session, "user", null);

        assertThat(session.isOwnedBy(1L)).isFalse();
    }

    private TrackingSession session() {
        TrackingSession session = TrackingSession.create(user(1L), mock(Mountain.class), null, true);
        ReflectionTestUtils.setField(session, "id", 10L);
        return session;
    }

    private User user(Long id) {
        User user = User.createTestUser("tracking-user-" + id, DeviceType.IOS);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
