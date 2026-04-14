package com.fuelix.repository;

import com.fuelix.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId OR n.notificationType = 'PUBLIC' ORDER BY n.createdAt DESC")
    List<Notification> getUserNotifications(@Param("userId") Long userId);
}