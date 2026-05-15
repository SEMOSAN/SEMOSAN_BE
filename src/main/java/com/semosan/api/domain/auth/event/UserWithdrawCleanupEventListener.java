package com.semosan.api.domain.auth.event;

import com.semosan.api.domain.auth.dispatcher.UserWithdrawCleanupDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserWithdrawCleanupEventListener {

    private final UserWithdrawCleanupDispatcher dispatcher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserWithdrawCleanupRequested(UserWithdrawCleanupRequestedEvent event) {
        dispatcher.dispatch(event);
    }
}
