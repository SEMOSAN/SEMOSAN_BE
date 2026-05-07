package com.semosan.api.domain.notification.repository;

import com.semosan.api.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// TODO: 알림판 구현할 때 쓸 예정
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
