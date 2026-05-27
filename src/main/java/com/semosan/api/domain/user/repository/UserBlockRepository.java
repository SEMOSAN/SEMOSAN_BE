package com.semosan.api.domain.user.repository;

import com.semosan.api.domain.user.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlocker_IdAndBlockedUser_Id(Long blockerId, Long blockedUserId);

    Optional<UserBlock> findByBlocker_IdAndBlockedUser_Id(Long blockerId, Long blockedUserId);

    @Query("SELECT ub.blockedUser.id FROM UserBlock ub WHERE ub.blocker.id = :blockerId")
    List<Long> findBlockedUserIdsByBlocker_Id(@Param("blockerId") Long blockerId);
}
