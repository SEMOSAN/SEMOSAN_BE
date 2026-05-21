package com.semosan.api.domain.notification.repository;

import com.semosan.api.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// TODO: 알림판 구현할 때 쓸 예정
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
