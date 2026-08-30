package com.semosan.api.domain.notification.dispatcher;

import com.google.firebase.ErrorCode;
import com.google.firebase.FirebaseException;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import com.semosan.api.common.fcm.FcmService;
import com.semosan.api.domain.notification.enums.NotificationType;
import com.semosan.api.domain.notification.service.FcmTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncNotificationDispatcherTest {

    @Mock
    private FcmService fcmService;

    @Mock
    private FcmTokenService fcmTokenService;

    @Test
    @SuppressWarnings("unchecked")
    void dispatchSendsTrackingNotificationAsDataOnlyWithTitleBodyAndDistance() throws Exception {
        AsyncNotificationDispatcher dispatcher = dispatcher();
        stubSuccessfulBatch(1);
        NotificationDispatchCommand command = new NotificationDispatchCommand(
                1L,
                10L,
                NotificationType.TRACKING_PHOTO_MILESTONE,
                "SEMOSAN",
                "500m 돌파! 인증 사진을 남겨보세요!",
                Map.of("distance", 500),
                List.of("token-1")
        );

        dispatcher.dispatch(command);

        ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(fcmService).sendEachForMulticast(
                eq(List.of("token-1")),
                eq("SEMOSAN"),
                eq("500m 돌파! 인증 사진을 남겨보세요!"),
                dataCaptor.capture(),
                eq(false)
        );
        assertThat(dataCaptor.getValue())
                .containsEntry("type", "TRACKING_PHOTO_MILESTONE")
                .containsEntry("title", "SEMOSAN")
                .containsEntry("body", "500m 돌파! 인증 사진을 남겨보세요!")
                .containsEntry("distance", "500")
                .containsEntry("notificationId", "1")
                .doesNotContainKey("extras");
    }

    @Test
    @SuppressWarnings("unchecked")
    void dispatchKeepsGeneralNotificationPayload() throws Exception {
        AsyncNotificationDispatcher dispatcher = dispatcher();
        stubSuccessfulBatch(1);
        NotificationDispatchCommand command = new NotificationDispatchCommand(
                2L,
                10L,
                NotificationType.COMMUNITY_COMMENT,
                "새 댓글이 달렸어요",
                "푸름: 확인했어요",
                Map.of("actorName", "푸름", "commentPreview", "확인했어요"),
                List.of("token-1")
        );

        dispatcher.dispatch(command);

        ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(fcmService).sendEachForMulticast(
                eq(List.of("token-1")),
                eq("새 댓글이 달렸어요"),
                eq("푸름: 확인했어요"),
                dataCaptor.capture(),
                eq(false)
        );
        assertThat(dataCaptor.getValue())
                .containsEntry("type", "COMMUNITY_COMMENT")
                .containsEntry("title", "새 댓글이 달렸어요")
                .containsEntry("body", "푸름: 확인했어요")
                .doesNotContainKey("extras");
    }

    @Test
    @SuppressWarnings("unchecked")
    void dispatchBuildsBasePayloadWhenExtrasAreNull() throws Exception {
        AsyncNotificationDispatcher dispatcher = dispatcher();
        stubSuccessfulBatch(1);
        NotificationDispatchCommand command = new NotificationDispatchCommand(
                3L,
                10L,
                NotificationType.TRACKING_SUMMIT_REACHED,
                "SEMOSAN",
                "정상에 도착했나요? 정상 인증하기!",
                null,
                List.of("token-1")
        );

        dispatcher.dispatch(command);

        ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(fcmService).sendEachForMulticast(
                eq(List.of("token-1")),
                eq("SEMOSAN"),
                eq("정상에 도착했나요? 정상 인증하기!"),
                dataCaptor.capture(),
                anyBoolean()
        );
        assertThat(dataCaptor.getValue())
                .containsEntry("type", "TRACKING_SUMMIT_REACHED")
                .containsEntry("title", "SEMOSAN")
                .containsEntry("body", "정상에 도착했나요? 정상 인증하기!")
                .containsEntry("notificationId", "3");
    }

    @Test
    @SuppressWarnings("unchecked")
    void dispatchIgnoresNullExtraKeysAndValues() throws Exception {
        AsyncNotificationDispatcher dispatcher = dispatcher();
        stubSuccessfulBatch(1);
        Map<String, Object> extras = new HashMap<>();
        extras.put("distance", 500);
        extras.put("ignored", null);
        extras.put(null, "ignored");
        NotificationDispatchCommand command = new NotificationDispatchCommand(
                4L,
                10L,
                NotificationType.TRACKING_PHOTO_MILESTONE,
                "SEMOSAN",
                "500m 돌파! 인증 사진을 남겨보세요!",
                extras,
                List.of("token-1")
        );

        dispatcher.dispatch(command);

        ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(fcmService).sendEachForMulticast(anyList(), anyString(), anyString(), dataCaptor.capture(), anyBoolean());
        assertThat(dataCaptor.getValue())
                .containsEntry("distance", "500")
                .doesNotContainKeys("ignored", null);
    }

    @Test
    void dispatchDeletesExpiredTokenWhenFirebaseReportsUnregistered() throws Exception {
        AsyncNotificationDispatcher dispatcher = dispatcher();
        NotificationDispatchCommand command = command(List.of("expired-token"));
        stubBatchWithFailure("expired-token", MessagingErrorCode.UNREGISTERED);

        dispatcher.dispatch(command);

        verify(fcmTokenService).deleteExpired("expired-token");
    }

    @Test
    void dispatchDeletesInvalidTokenWhenFirebaseReportsInvalidArgument() throws Exception {
        AsyncNotificationDispatcher dispatcher = dispatcher();
        NotificationDispatchCommand command = command(List.of("bad-token"));
        stubBatchWithFailure("bad-token", MessagingErrorCode.INVALID_ARGUMENT);

        dispatcher.dispatch(command);

        verify(fcmTokenService).deleteExpired("bad-token");
    }

    @Test
    void dispatchKeepsTokenWhenFirebaseReportsTransientError() throws Exception {
        AsyncNotificationDispatcher dispatcher = dispatcher();
        NotificationDispatchCommand command = command(List.of("retry-token"));
        stubBatchWithFailure("retry-token", MessagingErrorCode.UNAVAILABLE);

        dispatcher.dispatch(command);

        verify(fcmTokenService, never()).deleteExpired(anyString());
    }

    @Test
    void dispatchLogsUnexpectedFailedSendWithoutDeletingToken() throws Exception {
        AsyncNotificationDispatcher dispatcher = dispatcher();
        NotificationDispatchCommand command = command(List.of("broken-token"));
        SendResponse failed = mock(SendResponse.class);
        when(failed.isSuccessful()).thenReturn(false);
        when(failed.getException()).thenReturn(firebaseMessagingException(MessagingErrorCode.INTERNAL));
        BatchResponse batch = batchResponse(List.of(failed));
        when(fcmService.sendEachForMulticast(eq(List.of("broken-token")), anyString(), anyString(), anyMap(), anyBoolean()))
                .thenReturn(batch);

        dispatcher.dispatch(command);

        verify(fcmTokenService, never()).deleteExpired(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void dispatchContinuesNextChunkWhenOneChunkFailsUnexpectedly() throws Exception {
        AsyncNotificationDispatcher dispatcher = dispatcher();
        List<String> tokens = IntStream.range(0, 501)
                .mapToObj(i -> "token-" + i)
                .collect(Collectors.toCollection(ArrayList::new));
        when(fcmService.sendEachForMulticast(anyList(), anyString(), anyString(), anyMap(), anyBoolean()))
                .thenThrow(new IllegalStateException("boom")) // 첫 청크는 배치 호출 자체가 실패
                .thenAnswer(invocation -> { // 두 번째 청크는 정상 처리
                    List<String> chunk = invocation.getArgument(0);
                    return batchResponse(chunk.stream().map(t -> successResponse()).toList());
                });

        dispatcher.dispatch(command(tokens));

        verify(fcmService, times(2)).sendEachForMulticast(anyList(), anyString(), anyString(), anyMap(), anyBoolean());
    }

    @Test
    @SuppressWarnings("unchecked")
    void dispatchSplitsTokensIntoChunksOfAtMost500() throws Exception {
        AsyncNotificationDispatcher dispatcher = dispatcher();
        List<String> tokens = IntStream.range(0, 501)
                .mapToObj(i -> "token-" + i)
                .collect(Collectors.toCollection(ArrayList::new));
        when(fcmService.sendEachForMulticast(anyList(), anyString(), anyString(), anyMap(), anyBoolean()))
                .thenAnswer(invocation -> {
                    List<String> chunk = invocation.getArgument(0);
                    return batchResponse(chunk.stream().map(t -> successResponse()).toList());
                });

        dispatcher.dispatch(command(tokens));

        ArgumentCaptor<List<String>> chunkCaptor = ArgumentCaptor.forClass(List.class);
        verify(fcmService, times(2)).sendEachForMulticast(chunkCaptor.capture(), anyString(), anyString(), anyMap(), anyBoolean());
        List<List<String>> chunks = chunkCaptor.getAllValues();
        assertThat(chunks.get(0)).hasSize(500);
        assertThat(chunks.get(1)).hasSize(1);
    }

    private AsyncNotificationDispatcher dispatcher() {
        return new AsyncNotificationDispatcher(
                fcmService,
                fcmTokenService
        );
    }

    private NotificationDispatchCommand command(List<String> tokens) {
        return new NotificationDispatchCommand(
                1L,
                10L,
                NotificationType.COMMUNITY_COMMENT,
                "새 댓글이 달렸어요",
                "푸름: 확인했어요",
                Map.of("actorName", "푸름", "commentPreview", "확인했어요"),
                tokens
        );
    }

    private void stubSuccessfulBatch(int tokenCount) throws Exception {
        List<SendResponse> responses = new ArrayList<>();
        for (int i = 0; i < tokenCount; i++) {
            responses.add(successResponse());
        }
        BatchResponse batch = batchResponse(responses);
        when(fcmService.sendEachForMulticast(anyList(), anyString(), anyString(), anyMap(), anyBoolean()))
                .thenReturn(batch);
    }

    private void stubBatchWithFailure(String token, MessagingErrorCode errorCode) throws Exception {
        SendResponse failed = mock(SendResponse.class);
        when(failed.isSuccessful()).thenReturn(false);
        when(failed.getException()).thenReturn(firebaseMessagingException(errorCode));
        BatchResponse batch = batchResponse(List.of(failed));
        when(fcmService.sendEachForMulticast(eq(List.of(token)), anyString(), anyString(), anyMap(), anyBoolean()))
                .thenReturn(batch);
    }

    private SendResponse successResponse() {
        SendResponse success = mock(SendResponse.class);
        when(success.isSuccessful()).thenReturn(true);
        return success;
    }

    private BatchResponse batchResponse(List<SendResponse> responses) {
        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getResponses()).thenReturn(responses);
        return batchResponse;
    }

    private FirebaseMessagingException firebaseMessagingException(MessagingErrorCode messagingErrorCode)
            throws Exception {
        FirebaseException firebaseException = new FirebaseException(ErrorCode.UNKNOWN, "error", null);
        Method method = FirebaseMessagingException.class.getDeclaredMethod(
                "withMessagingErrorCode",
                FirebaseException.class,
                MessagingErrorCode.class
        );
        method.setAccessible(true);
        return (FirebaseMessagingException) method.invoke(null, firebaseException, messagingErrorCode);
    }
}
