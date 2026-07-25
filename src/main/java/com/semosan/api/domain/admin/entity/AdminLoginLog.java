package com.semosan.api.domain.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_login_logs")
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminLoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "fail_reason", length = 255)
    private String failReason;

    @Column(name = "attempted_at", nullable = false, updatable = false)
    private LocalDateTime attemptedAt;

    public static AdminLoginLog success(String username, String ipAddress, String userAgent) {
        return AdminLoginLog.builder()
                .username(username)
                .success(true)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .attemptedAt(LocalDateTime.now())
                .build();
    }

    public static AdminLoginLog fail(String username, String ipAddress, String userAgent, String failReason) {
        return AdminLoginLog.builder()
                .username(username)
                .success(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .failReason(failReason)
                .attemptedAt(LocalDateTime.now())
                .build();
    }
}
