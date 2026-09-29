package com.semosan.api.common.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class TransactionSynchronizationLeakDetectorTest {

    private final TransactionSynchronizationLeakDetector detector = new TransactionSynchronizationLeakDetector();
    private final ExtensionContext context = Mockito.mock(ExtensionContext.class);

    {
        Mockito.lenient().when(context.getUniqueId()).thenReturn("[test:leak-detector-self-test]");
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void beforeEachClearsSynchronizationLeakedByPreviousTest() {
        TransactionSynchronizationManager.initSynchronization();

        detector.beforeEach(context);

        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    }

    @Test
    void beforeEachDoesNothingWhenNoSynchronizationIsActive() {
        assertThatCode(() -> detector.beforeEach(context)).doesNotThrowAnyException();

        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    }

    @Test
    void afterEachLeavesSynchronizationUntouchedSoTheOwningTestCanStillCleanUp() {
        TransactionSynchronizationManager.initSynchronization();

        detector.afterEach(context);

        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
    }
}
