package com.semosan.api.domain.hiking.repository;

import com.semosan.api.domain.hiking.entity.HikingMember;
import com.semosan.api.domain.hiking.entity.HikingRecord;
import com.semosan.api.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HikingMemberRepository extends JpaRepository<HikingMember, Long> {

    boolean existsByHikingRecordAndUser(HikingRecord hikingRecord, User user);

    boolean existsByUser_Id(Long userId);

    List<HikingMember> findByHikingRecord(HikingRecord hikingRecord);

    Page<HikingMember> findByUser(User user, Pageable pageable);
}
