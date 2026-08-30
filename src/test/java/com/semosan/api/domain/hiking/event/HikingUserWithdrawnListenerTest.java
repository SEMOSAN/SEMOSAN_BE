package com.semosan.api.domain.hiking.event;

import com.semosan.api.domain.hiking.repository.CourseDifficultyFeedbackRepository;
import com.semosan.api.domain.hiking.repository.HikingMemberRepository;
import com.semosan.api.domain.hiking.repository.HikingRecordRepository;
import com.semosan.api.domain.user.event.UserWithdrawnEvent;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HikingUserWithdrawnListenerTest {

    @Test
    void onUserWithdrawnDeletesInOrderAndBatchDeletesOnlyParticipatedRecords() {
        HikingMemberRepository hikingMemberRepository = mock(HikingMemberRepository.class);
        HikingRecordRepository hikingRecordRepository = mock(HikingRecordRepository.class);
        CourseDifficultyFeedbackRepository courseDifficultyFeedbackRepository =
                mock(CourseDifficultyFeedbackRepository.class);
        when(hikingRecordRepository.findRecordIdsOnlyParticipatedByUser(1L)).thenReturn(List.of(10L, 11L));
        HikingUserWithdrawnListener listener = new HikingUserWithdrawnListener(
                hikingMemberRepository, hikingRecordRepository, courseDifficultyFeedbackRepository);

        listener.onUserWithdrawn(new UserWithdrawnEvent(1L));

        verify(courseDifficultyFeedbackRepository).deleteByUserId(1L);
        verify(hikingMemberRepository).deleteByUser_Id(1L);
        verify(courseDifficultyFeedbackRepository).deleteByHikingRecordIdIn(List.of(10L, 11L));
        verify(hikingRecordRepository).deleteAllByIdInBatch(List.of(10L, 11L));

        InOrder order = inOrder(courseDifficultyFeedbackRepository, hikingRecordRepository, hikingMemberRepository);
        order.verify(courseDifficultyFeedbackRepository).deleteByUserId(1L);
        order.verify(hikingRecordRepository).findRecordIdsOnlyParticipatedByUser(1L);
        order.verify(hikingMemberRepository).deleteByUser_Id(1L);
        order.verify(courseDifficultyFeedbackRepository).deleteByHikingRecordIdIn(List.of(10L, 11L));
        order.verify(hikingRecordRepository).deleteAllByIdInBatch(List.of(10L, 11L));
    }

    @Test
    void onUserWithdrawnSkipsBatchDeleteWhenNoOnlyParticipatedRecords() {
        HikingMemberRepository hikingMemberRepository = mock(HikingMemberRepository.class);
        HikingRecordRepository hikingRecordRepository = mock(HikingRecordRepository.class);
        CourseDifficultyFeedbackRepository courseDifficultyFeedbackRepository =
                mock(CourseDifficultyFeedbackRepository.class);
        when(hikingRecordRepository.findRecordIdsOnlyParticipatedByUser(1L)).thenReturn(List.of());
        HikingUserWithdrawnListener listener = new HikingUserWithdrawnListener(
                hikingMemberRepository, hikingRecordRepository, courseDifficultyFeedbackRepository);

        listener.onUserWithdrawn(new UserWithdrawnEvent(1L));

        verify(courseDifficultyFeedbackRepository, never()).deleteByHikingRecordIdIn(any());
        verify(hikingRecordRepository, never()).deleteAllByIdInBatch(any());
    }
}
