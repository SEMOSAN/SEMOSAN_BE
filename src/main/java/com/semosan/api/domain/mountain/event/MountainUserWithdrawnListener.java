package com.semosan.api.domain.mountain.event;

import com.semosan.api.domain.mountain.repository.CourseLikeRepository;
import com.semosan.api.domain.mountain.repository.MountainLikeRepository;
import com.semosan.api.domain.user.event.UserWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MountainUserWithdrawnListener {

    private final MountainLikeRepository mountainLikeRepository;
    private final CourseLikeRepository courseLikeRepository;

    // 탈퇴 트랜잭션과 원자적으로 처리되어야 하므로 BEFORE_COMMIT으로 같은 트랜잭션 안에서 실행한다.
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onUserWithdrawn(UserWithdrawnEvent event) {
        mountainLikeRepository.deleteByUser_Id(event.userId());
        courseLikeRepository.deleteByUser_Id(event.userId());
    }
}
