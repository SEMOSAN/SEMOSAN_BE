package com.semosan.api.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LikeConflictHandlerTest {

    @Test
    void handleConcurrentCreateReturnsTrueWhenCreateActionSucceeds() {
        AtomicBoolean createCalled = new AtomicBoolean(false);
        AtomicBoolean logCalled = new AtomicBoolean(false);

        boolean result = LikeConflictHandler.handleConcurrentCreate(
                () -> createCalled.set(true),
                () -> logCalled.set(true)
        );

        assertThat(result).isTrue();
        assertThat(createCalled).isTrue();
        assertThat(logCalled).isFalse();
    }

    @Test
    void handleConcurrentCreateReturnsTrueAndRunsLogActionWhenDataIntegrityViolationOccurs() {
        AtomicBoolean logCalled = new AtomicBoolean(false);

        boolean result = LikeConflictHandler.handleConcurrentCreate(
                () -> {
                    throw new DataIntegrityViolationException("duplicate");
                },
                () -> logCalled.set(true)
        );

        assertThat(result).isTrue();
        assertThat(logCalled).isTrue();
    }

    @Test
    void handleConcurrentCreateRethrowsUnexpectedException() {
        RuntimeException exception = new IllegalStateException("unexpected");

        assertThatThrownBy(() -> LikeConflictHandler.handleConcurrentCreate(
                () -> {
                    throw exception;
                },
                () -> {}
        )).isSameAs(exception);
    }
}
