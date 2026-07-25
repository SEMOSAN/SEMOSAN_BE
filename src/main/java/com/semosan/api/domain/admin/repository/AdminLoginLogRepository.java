package com.semosan.api.domain.admin.repository;

import com.semosan.api.domain.admin.entity.AdminLoginLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminLoginLogRepository extends JpaRepository<AdminLoginLog, Long> {
}
