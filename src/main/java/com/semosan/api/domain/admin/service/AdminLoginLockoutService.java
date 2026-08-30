package com.semosan.api.domain.admin.service;

import com.semosan.api.domain.admin.repository.AdminLoginLockoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * REQUIRES_NEW로 항상 독립 커밋되게 한다 — login()이 자격증명 검증 실패로
 * 예외를 던져도(트랜잭션 롤백) 잠금 카운터 증가는 살아남아야 하기 때문이다.
 * AdminLoginLogService와 동일한 이유의 동일한 패턴.
 */
@Service
@RequiredArgsConstructor
public class AdminLoginLockoutService {

    private final AdminLoginLockoutRepository adminLoginLockoutRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recordAttempt(String username, LocalDateTime windowStart) {
        return adminLoginLockoutRepository.recordAttemptAndGetCount(username, windowStart);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reset(String username) {
        adminLoginLockoutRepository.reset(username);
    }
}
