package com.semosan.api.domain.user.repository;

import com.semosan.api.domain.user.entity.UserOnboarding;
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

    void deleteByUser_Id(Long userId);
}
