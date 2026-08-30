package com.semosan.api.domain.user.repository;

import com.semosan.api.domain.user.entity.UserOnboarding;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserOnboardingRepository extends JpaRepository<UserOnboarding, Long> {

    boolean existsByUser_Id(Long userId);

    Optional<UserOnboarding> findByUser_Id(Long userId);

    @Query("""
            SELECT uo
            FROM UserOnboarding uo
            JOIN FETCH uo.user u
            WHERE u.id = :userId
              AND u.deleted = false
            """)
    Optional<UserOnboarding> findByUserIdWithUser(@Param("userId") Long userId);

    // 일반 derived delete는 flush에 의존해 다른 리포지토리의 clearAutomatically=true 호출에
    // 유실될 수 있어(#393), 다른 도메인들과 동일하게 즉시 실행되는 벌크 쿼리로 통일한다.
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserOnboarding uo WHERE uo.user.id = :userId")
    void deleteByUser_Id(@Param("userId") Long userId);
}
