package com.semosan.api.domain.tracking.service;

import com.semosan.api.domain.notification.enums.NotificationType;
import com.semosan.api.domain.notification.service.NotificationService;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingMilestoneTriggerServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TrackingMilestoneCalculator milestoneCalculator;

    @Test
    void initializeMilestonesReturnsWhenCalculatedMilestonesAreEmpty() {
        TrackingSession session = mock(TrackingSession.class);
        when(milestoneCalculator.calculate(session)).thenReturn(List.of());

        service().initializeMilestones(session);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void initializeMilestonesSavesCalculatedMilestonesWithTtl() {
        TrackingSession session = mock(TrackingSession.class);
        when(session.getId()).thenReturn(1L);
        when(milestoneCalculator.calculate(session)).thenReturn(List.of(100.0, 200.0));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service().initializeMilestones(session);

        verify(valueOperations).set("tracking:session:1:milestones", "100.0,200.0");
        verify(redisTemplate).expire("tracking:session:1:milestones", Duration.ofHours(24));
    }

    @Test
    void evaluateReturnsWhenMilestonesAreMissing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("tracking:session:1:milestones")).thenReturn(null);

        service().evaluate(1L, 10L, 100.0);

        verifyNoInteractions(messagingTemplate, notificationService);
    }

    @Test
    void evaluateReturnsWhenMilestonesAreBlank() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("tracking:session:1:milestones")).thenReturn(" ");

        service().evaluate(1L, 10L, 100.0);

        verifyNoInteractions(messagingTemplate, notificationService);
    }

    @Test
    void evaluateOpensPhotoWindowAndSendsCourseModeNotification() {
        mockLoadedMilestones("100.0,200.0,300.0,400.0", Set.of(), Set.of());

        service().evaluate(1L, 10L, 90.0);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/tracking/1/photo-window"), (Object) payloadCaptor.capture());
        assertThat(payloadCaptor.getValue())
                .containsEntry("milestoneIndex", 0)
                .containsEntry("milestoneDistance", 100.0)
                .containsEntry("status", "OPEN");
        verify(notificationService).send(
                eq(10L),
                eq(NotificationType.TRACKING_PHOTO_MILESTONE),
                eq(Map.of("distance", 100)),
                eq("정상 도착_1/4 완료_눌러서 인증 남기기")
        );
        verify(setOperations).add("tracking:session:1:photo:opened", "0");
        verify(redisTemplate).expire("tracking:session:1:photo:opened", Duration.ofHours(24));
    }

    @Test
    void evaluateUsesDistanceBodyWhenMilestoneCountIsNotCourseMode() {
        mockLoadedMilestones("100.0,200.0,300.0,400.0,500.0,600.0", Set.of(), Set.of());

        service().evaluate(1L, 10L, 90.0);

        verify(notificationService).send(
                eq(10L),
                eq(NotificationType.TRACKING_PHOTO_MILESTONE),
                eq(Map.of("distance", 100)),
                eq("100m 돌파! 인증 사진을 남겨보세요!")
        );
    }

    @Test
    void evaluateUsesCourseModeBodiesForLaterMilestones() {
        assertThat((String) ReflectionTestUtils.invokeMethod(
                TrackingMilestoneTriggerService.class,
                "courseModeBody",
                4,
                1,
                200
        )).isEqualTo("정상 도착_절반 돌파_눌러서 인증 남기기");
        assertThat((String) ReflectionTestUtils.invokeMethod(
                TrackingMilestoneTriggerService.class,
                "courseModeBody",
                4,
                2,
                300
        )).isEqualTo("정상 도착_마지막 1/4_눌러서 인증 남기기");
        assertThat((String) ReflectionTestUtils.invokeMethod(
                TrackingMilestoneTriggerService.class,
                "courseModeBody",
                4,
                3,
                400
        )).isEqualTo("정상 도착_완료_진짜최종_눌러서 인증하기");
        assertThat((String) ReflectionTestUtils.invokeMethod(
                TrackingMilestoneTriggerService.class,
                "courseModeBody",
                4,
                99,
                900
        )).isEqualTo("900m 돌파! 인증 사진을 남겨보세요!");
    }

    @Test
    void evaluateTreatsNullOpenedAndClosedMembersAsEmptySets() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(valueOperations.get("tracking:session:1:milestones")).thenReturn("100.0,200.0,300.0,400.0");
        when(setOperations.members("tracking:session:1:photo:opened")).thenReturn(null);
        when(setOperations.members("tracking:session:1:photo:closed")).thenReturn(null);

        service().evaluate(1L, 10L, 90.0);

        verify(messagingTemplate).convertAndSend(eq("/topic/tracking/1/photo-window"), (Object) any(Map.class));
        verify(setOperations).add("tracking:session:1:photo:opened", "0");
    }

    @Test
    void evaluateKeepsOpeningPhotoWindowWhenNotificationFails() {
        mockLoadedMilestones("100.0,200.0,300.0,400.0", Set.of(), Set.of());
        doThrow(new RuntimeException("fcm fail"))
                .when(notificationService)
                .send(eq(10L), eq(NotificationType.TRACKING_PHOTO_MILESTONE), any(), any());

        service().evaluate(1L, 10L, 90.0);

        verify(messagingTemplate).convertAndSend(eq("/topic/tracking/1/photo-window"), (Object) any(Map.class));
        verify(setOperations).add("tracking:session:1:photo:opened", "0");
    }

    @Test
    void evaluateClosesPhotoWindowWhenOpenedWindowPassedExitDistance() {
        mockLoadedMilestones("100.0,200.0,300.0,400.0", Set.of("0"), Set.of());

        service().evaluate(1L, 10L, 111.0);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/tracking/1/photo-window"), (Object) payloadCaptor.capture());
        assertThat(payloadCaptor.getValue())
                .containsEntry("milestoneIndex", 0)
                .containsEntry("milestoneDistance", 100.0)
                .containsEntry("status", "CLOSED");
        verify(setOperations).add("tracking:session:1:photo:closed", "0");
        verify(redisTemplate).expire("tracking:session:1:photo:closed", Duration.ofHours(24));
    }

    @Test
    void evaluateDoesNotClosePhotoWindowWhenAlreadyClosed() {
        mockLoadedMilestones("100.0,200.0,300.0,400.0", Set.of("0"), Set.of("0"));

        service().evaluate(1L, 10L, 111.0);

        verify(messagingTemplate, never()).convertAndSend(eq("/topic/tracking/1/photo-window"), (Object) any(Map.class));
        verify(setOperations, never()).add("tracking:session:1:photo:closed", "0");
    }

    @Test
    void evaluateSummitReturnsWhenCourseDistanceIsNotPositive() {
        service().evaluateSummit(1L, 10L, 100.0, 0.0);

        verifyNoInteractions(redisTemplate, messagingTemplate, notificationService);
    }

    @Test
    void evaluateSummitReturnsBeforeHalfwayMark() {
        service().evaluateSummit(1L, 10L, 199.0, 400.0);

        verifyNoInteractions(redisTemplate, messagingTemplate, notificationService);
    }

    @Test
    void evaluateSummitReturnsWhenAlreadyNotified() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add("tracking:session:1:summit:notified", "1")).thenReturn(0L);

        service().evaluateSummit(1L, 10L, 200.0, 400.0);

        verify(redisTemplate, never()).expire("tracking:session:1:summit:notified", Duration.ofHours(24));
        verifyNoInteractions(messagingTemplate, notificationService);
    }

    @Test
    void evaluateSummitReturnsWhenRedisAddReturnsNull() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add("tracking:session:1:summit:notified", "1")).thenReturn(null);

        service().evaluateSummit(1L, 10L, 200.0, 400.0);

        verifyNoInteractions(messagingTemplate, notificationService);
    }

    @Test
    void evaluateSummitSendsWebSocketAndNotificationOnce() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add("tracking:session:1:summit:notified", "1")).thenReturn(1L);

        service().evaluateSummit(1L, 10L, 200.0, 400.0);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(redisTemplate).expire("tracking:session:1:summit:notified", Duration.ofHours(24));
        verify(messagingTemplate).convertAndSend(eq("/topic/tracking/1/summit"), (Object) payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).containsEntry("halfwayMark", 200.0);
        verify(notificationService).send(10L, NotificationType.TRACKING_SUMMIT_REACHED, Map.of());
    }

    @Test
    void evaluateSummitKeepsWebSocketWhenNotificationFails() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add("tracking:session:1:summit:notified", "1")).thenReturn(1L);
        doThrow(new RuntimeException("fcm fail"))
                .when(notificationService)
                .send(10L, NotificationType.TRACKING_SUMMIT_REACHED, Map.of());

        service().evaluateSummit(1L, 10L, 200.0, 400.0);

        verify(messagingTemplate).convertAndSend(eq("/topic/tracking/1/summit"), (Object) any(Map.class));
    }

    private void mockLoadedMilestones(String rawMilestones, Set<String> opened, Set<String> closed) {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(valueOperations.get("tracking:session:1:milestones")).thenReturn(rawMilestones);
        when(setOperations.members("tracking:session:1:photo:opened")).thenReturn(opened);
        when(setOperations.members("tracking:session:1:photo:closed")).thenReturn(closed);
    }

    @Test
    void getMilestoneStateReturnsSortedIndexesAndSummitFlag() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("tracking:session:1:milestones")).thenReturn("100.0,200.0,300.0");
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        // Redis Set 은 순서를 보장하지 않으므로 일부러 뒤섞어 반환한다.
        when(setOperations.members("tracking:session:1:photo:opened")).thenReturn(Set.of("2", "0", "1"));
        when(setOperations.members("tracking:session:1:photo:closed")).thenReturn(Set.of("1", "0"));
        when(setOperations.members("tracking:session:1:summit:notified")).thenReturn(Set.of("1"));

        TrackingMilestoneTriggerService.MilestoneState state = service().getMilestoneState(1L);

        assertThat(state.milestones()).containsExactly(100.0, 200.0, 300.0);
        assertThat(state.openedIndexes()).containsExactly(0, 1, 2);
        assertThat(state.closedIndexes()).containsExactly(0, 1);
        assertThat(state.summitNotified()).isTrue();
    }

    @Test
    void getMilestoneStateReturnsEmptyStateWhenNothingStored() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("tracking:session:1:milestones")).thenReturn(null);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(any())).thenReturn(null);

        TrackingMilestoneTriggerService.MilestoneState state = service().getMilestoneState(1L);

        assertThat(state.milestones()).isEmpty();
        assertThat(state.openedIndexes()).isEmpty();
        assertThat(state.closedIndexes()).isEmpty();
        assertThat(state.summitNotified()).isFalse();
    }

    private TrackingMilestoneTriggerService service() {
        return new TrackingMilestoneTriggerService(
                redisTemplate,
                messagingTemplate,
                notificationService,
                milestoneCalculator
        );
    }
}
