package com.semosan.api.common.util;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.function.Consumer;

public final class LikeConflictHandler {

    private LikeConflictHandler() {}

    /**
     * IDENTITY 전략은 insert 실패 시 세션에 id 없는 엔티티가 남아, 이후 flush에서
     * AssertionFailure(null identifier)로 이어진다. onConflict에서 반드시
     * EntityManager#clear()로 오염된 영속성 컨텍스트를 정리해야 한다.
     */
    public static boolean handleConcurrentCreate(Runnable createAction, Consumer<DataIntegrityViolationException> onConflict) {
        try {
            createAction.run();
            return true;
        } catch (DataIntegrityViolationException e) {
            onConflict.accept(e);
            return true;
        }
    }
}
