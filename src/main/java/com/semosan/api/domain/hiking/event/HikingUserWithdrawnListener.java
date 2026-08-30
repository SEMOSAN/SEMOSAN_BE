package com.semosan.api.domain.hiking.event;

import com.semosan.api.domain.hiking.repository.CourseDifficultyFeedbackRepository;
import com.semosan.api.domain.hiking.repository.HikingMemberRepository;
import com.semosan.api.domain.hiking.repository.HikingRecordRepository;
import com.semosan.api.domain.user.event.UserWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HikingUserWithdrawnListener {

    private final HikingMemberRepository hikingMemberRepository;
    private final HikingRecordRepository hikingRecordRepository;
    private final CourseDifficultyFeedbackRepository courseDifficultyFeedbackRepository;

    // 탈퇴 트랜잭션과 원자적으로 처리되어야 하므로 BEFORE_COMMIT으로 같은 트랜잭션 안에서 실행한다.
    // 본인이 유일한 참여자인 기록의 ID는 멤버 삭제 전에 조회해야 하므로 순서를 바꾸면 안 된다.
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onUserWithdrawn(UserWithdrawnEvent event) {
        Long userId = event.userId();
        courseDifficultyFeedbackRepository.deleteByUserId(userId);
        List<Long> recordIdsToDelete = hikingRecordRepository.findRecordIdsOnlyParticipatedByUser(userId);
        hikingMemberRepository.deleteByUser_Id(userId);
        if (!recordIdsToDelete.isEmpty()) {
            courseDifficultyFeedbackRepository.deleteByHikingRecordIdIn(recordIdsToDelete);
            hikingRecordRepository.deleteAllByIdInBatch(recordIdsToDelete);
        }
    }
}
