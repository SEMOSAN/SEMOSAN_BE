package com.semosan.api.domain.hiking.repository;

import com.semosan.api.domain.hiking.entity.HikingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HikingRecordRepository extends JpaRepository<HikingRecord, Long> {
}
