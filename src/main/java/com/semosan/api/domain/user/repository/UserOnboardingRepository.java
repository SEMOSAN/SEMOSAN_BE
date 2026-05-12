package com.semosan.api.domain.user.repository;

import com.semosan.api.domain.user.entity.UserOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserOnboardingRepository extends JpaRepository<UserOnboarding, Long> {

    boolean existsByUser_Id(Long userId);

    Optional<UserOnboarding> findByUser_Id(Long userId);
}
