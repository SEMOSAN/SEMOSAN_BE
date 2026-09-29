package com.semosan.api.common.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 테스트가 스레드에 남긴 트랜잭션 동기화를 잡아내고, 뒤따르는 테스트로 번지지 않게 정리한다. (#447)
 *
 * RedisConfig.cacheManager() 가 transactionAware 라서, 동기화가 활성화된 스레드에서 발생한
 * 캐시 PUT 은 커밋 시점까지 미뤄진다. 앞선 테스트가 동기화를 남기면 뒤 테스트의 캐시 쓰기가
 * 영영 반영되지 않아, 원인과 무관한 테스트가 실패한다.
 *
 * 실제 트랜잭션이 붙어 있는 동기화(@Transactional 테스트가 정상적으로 연 것) 는 건드리지 않는다.
 * 수동 initSynchronization() 누수만 대상으로 한다.
 *
 * junit-platform.properties 의 extensions.autodetection 으로 전체 테스트에 자동 적용된다.
 */
@Slf4j
public class TransactionSynchronizationLeakDetector implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        if (isLeaked()) {
            log.warn("이전 테스트가 남긴 트랜잭션 동기화를 정리한다 (다음 테스트: {})", context.getUniqueId());
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (isLeaked()) {
            log.warn("이 테스트가 트랜잭션 동기화를 정리하지 않았다: {}", context.getUniqueId());
        }
    }

    private boolean isLeaked() {
        return TransactionSynchronizationManager.isSynchronizationActive()
                && !TransactionSynchronizationManager.isActualTransactionActive();
    }
}
