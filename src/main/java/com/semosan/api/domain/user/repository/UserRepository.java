package com.semosan.api.domain.user.repository;

import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByOauthIdAndOauthProvider(String oauthId, OAuthProvider oauthProvider);

    Optional<User> findByIdAndDeletedFalse(Long id);

    boolean existsByNicknameAndDeletedFalse(String nickname);
}
