package com.semosan.api.domain.notification.repository;

import com.semosan.api.domain.notification.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByToken(String token);

    List<FcmToken> findAllByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM FcmToken f WHERE f.token = :token")
    void deleteByToken(@Param("token") String token);

    @Modifying
    @Query("DELETE FROM FcmToken f WHERE f.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
