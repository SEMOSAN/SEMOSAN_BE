package com.semosan.api.domain.hiking.repository;

import com.semosan.api.domain.hiking.entity.HikingMember;
import com.semosan.api.domain.hiking.entity.HikingRecord;
import com.semosan.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.List;

public interface HikingMemberRepository extends JpaRepository<HikingMember, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM HikingMember hm WHERE hm.user.id = :userId")
    void deleteByUser_Id(@Param("userId") Long userId);

    boolean existsByHikingRecordAndUser(HikingRecord hikingRecord, User user);
    List<HikingMember> findByHikingRecord(HikingRecord hikingRecord);
    Page<HikingMember> findByUser(User user, Pageable pageable);
}
