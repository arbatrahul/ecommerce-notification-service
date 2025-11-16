package org.example.notification.repository;

import org.example.notification.entity.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    
    List<NotificationLog> findByUserId(Long userId);
    
    Page<NotificationLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<NotificationLog> findByStatus(NotificationLog.NotificationStatus status);
    
    List<NotificationLog> findByNotificationType(NotificationLog.NotificationType notificationType);
    
    List<NotificationLog> findByRecipientEmail(String recipientEmail);
    
    @Query("SELECT n FROM NotificationLog n WHERE n.status = :status AND n.createdAt < :beforeTime")
    List<NotificationLog> findFailedNotificationsOlderThan(@Param("status") NotificationLog.NotificationStatus status, 
                                                           @Param("beforeTime") LocalDateTime beforeTime);
    
    @Query("SELECT n FROM NotificationLog n WHERE n.orderId = :orderId")
    List<NotificationLog> findByOrderId(@Param("orderId") Long orderId);
    
    @Query("SELECT COUNT(n) FROM NotificationLog n WHERE n.status = :status AND n.createdAt >= :fromDate")
    Long countByStatusAndCreatedAtAfter(@Param("status") NotificationLog.NotificationStatus status, 
                                       @Param("fromDate") LocalDateTime fromDate);
    
    @Query("SELECT n.notificationType, COUNT(n) FROM NotificationLog n WHERE n.createdAt >= :fromDate GROUP BY n.notificationType")
    List<Object[]> getNotificationStatsByType(@Param("fromDate") LocalDateTime fromDate);
}
