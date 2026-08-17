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
    void elapsedSecondsExcludesAccumulatedPausedTimeWhileInProgress() {
        TrackingSession session = session();
        ReflectionTestUtils.setField(session, "startedAt", LocalDateTime.now().minusSeconds(600));
        ReflectionTestUtils.setField(session, "pausedSecondsTotal", 120);

        assertThat(session.elapsedSeconds()).isBetween(478L, 482L);
    }

    @Test
    void elapsedSecondsExcludesOngoingPauseWhilePaused() {
        TrackingSession session = session();
        ReflectionTestUtils.setField(session, "startedAt", LocalDateTime.now().minusSeconds(600));
        ReflectionTestUtils.setField(session, "pausedSecondsTotal", 60);
        session.pause();
        // 아직 pausedSecondsTotal 에 누적되지 않은 "진행 중인 일시정지" 구간 100초.
        ReflectionTestUtils.setField(session, "pausedAt", LocalDateTime.now().minusSeconds(100));

        assertThat(session.elapsedSeconds()).isBetween(438L, 442L);
    }

    @Test
    void elapsedSecondsStopsGrowingWhilePaused() throws InterruptedException {
        TrackingSession session = session();
        ReflectionTestUtils.setField(session, "startedAt", LocalDateTime.now().minusSeconds(600));
        session.pause();

        long first = session.elapsedSeconds();
        Thread.sleep(1_100);
        long second = session.elapsedSeconds();

        // 일시정지 중에는 시간이 흐르지 않아야 한다 — 복원 시 가장 틀리기 쉬운 지점.
        assertThat(second).isEqualTo(first);
    }

    @Test
    void elapsedSecondsUsesEndedAtForTerminatedSession() {
        TrackingSession session = session();
        LocalDateTime startedAt = LocalDateTime.now().minusHours(3);
        ReflectionTestUtils.setField(session, "startedAt", startedAt);
        session.complete();
        ReflectionTestUtils.setField(session, "endedAt", startedAt.plusSeconds(3_000));
        ReflectionTestUtils.setField(session, "pausedSecondsTotal", 300);

        assertThat(session.elapsedSeconds()).isEqualTo(2_700L);
    }

    @Test
    void elapsedSecondsIgnoresPausedAtWhenStatusIsNotPaused() {
        TrackingSession session = session();
        ReflectionTestUtils.setField(session, "startedAt", LocalDateTime.now().minusSeconds(300));
        // 상태는 IN_PROGRESS 인데 pausedAt 만 남아 있는 비정상 데이터 — 차감하지 않는다.
        ReflectionTestUtils.setField(session, "pausedAt", LocalDateTime.now().minusSeconds(100));

        assertThat(session.elapsedSeconds()).isBetween(298L, 302L);
    }

    @Test
    void elapsedSecondsNeverReturnsNegative() {
        TrackingSession session = session();
        ReflectionTestUtils.setField(session, "startedAt", LocalDateTime.now().minusSeconds(10));
        ReflectionTestUtils.setField(session, "pausedSecondsTotal", 9_999);

        assertThat(session.elapsedSeconds()).isZero();
    }

    @Test
    void elapsedSecondsTreatsNullPausedSecondsTotalAsZero() {
        TrackingSession session = session();
        ReflectionTestUtils.setField(session, "startedAt", LocalDateTime.now().minusSeconds(200));
        ReflectionTestUtils.setField(session, "pausedSecondsTotal", null);

        assertThat(session.elapsedSeconds()).isBetween(198L, 202L);
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
