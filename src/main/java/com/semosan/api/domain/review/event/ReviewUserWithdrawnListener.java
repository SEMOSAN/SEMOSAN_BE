package com.semosan.api.domain.review.event;

import com.semosan.api.domain.review.repository.ReviewRepository;
import com.semosan.api.domain.user.event.UserWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ReviewUserWithdrawnListener {

    private final ReviewRepository reviewRepository;

    // 탈퇴 트랜잭션과 원자적으로 처리되어야 하므로 BEFORE_COMMIT으로 같은 트랜잭션 안에서 실행한다.
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onUserWithdrawn(UserWithdrawnEvent event) {
        reviewRepository.deleteByUser_Id(event.userId());
    }
}
