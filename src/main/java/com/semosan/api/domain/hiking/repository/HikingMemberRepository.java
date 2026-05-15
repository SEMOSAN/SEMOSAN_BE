package com.semosan.api.domain.hiking.repository;

import com.semosan.api.domain.hiking.entity.HikingMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HikingMemberRepository extends JpaRepository<HikingMember, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM HikingMember hm WHERE hm.user.id = :userId")
    void deleteByUser_Id(@Param("userId") Long userId);
}
