package com.semosan.api.common.util;

import org.springframework.dao.DataIntegrityViolationException;

public final class LikeConflictHandler {

    private LikeConflictHandler() {}

    public static boolean handleConcurrentCreate(Runnable createAction, Runnable conflictLogAction) {
        try {
            createAction.run();
            return true;
        } catch (DataIntegrityViolationException e) {
            conflictLogAction.run();
            return true;
        }
    }
}
