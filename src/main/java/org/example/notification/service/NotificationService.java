package org.example.notification.service;

import org.example.notification.entity.NotificationLog;
import org.example.notification.repository.NotificationLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class NotificationService {

    @Autowired
    private NotificationLogRepository notificationLogRepository;
    
    @Autowired
    private EmailService emailService;

    public Page<NotificationLog> getUserNotifications(Long userId, Pageable pageable) {
        return notificationLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public List<NotificationLog> getFailedNotifications() {
        return notificationLogRepository.findByStatus(NotificationLog.NotificationStatus.FAILED);
    }

    public Map<String, Object> getNotificationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        LocalDateTime last7Days = LocalDateTime.now().minusDays(7);
        LocalDateTime last30Days = LocalDateTime.now().minusDays(30);
        
        // Count by status for different time periods
        stats.put("sent_last_24h", notificationLogRepository.countByStatusAndCreatedAtAfter(
            NotificationLog.NotificationStatus.SENT, last24Hours));
        stats.put("failed_last_24h", notificationLogRepository.countByStatusAndCreatedAtAfter(
            NotificationLog.NotificationStatus.FAILED, last24Hours));
        
        stats.put("sent_last_7d", notificationLogRepository.countByStatusAndCreatedAtAfter(
            NotificationLog.NotificationStatus.SENT, last7Days));
        stats.put("failed_last_7d", notificationLogRepository.countByStatusAndCreatedAtAfter(
            NotificationLog.NotificationStatus.FAILED, last7Days));
        
        stats.put("sent_last_30d", notificationLogRepository.countByStatusAndCreatedAtAfter(
            NotificationLog.NotificationStatus.SENT, last30Days));
        stats.put("failed_last_30d", notificationLogRepository.countByStatusAndCreatedAtAfter(
            NotificationLog.NotificationStatus.FAILED, last30Days));
        
        // Get notification type statistics
        List<Object[]> typeStats = notificationLogRepository.getNotificationStatsByType(last30Days);
        Map<String, Long> typeStatsMap = new HashMap<>();
        for (Object[] stat : typeStats) {
            typeStatsMap.put(stat[0].toString(), (Long) stat[1]);
        }
        stats.put("type_stats_last_30d", typeStatsMap);
        
        return stats;
    }

    public void retryNotification(Long notificationId) {
        Optional<NotificationLog> logOpt = notificationLogRepository.findById(notificationId);
        if (logOpt.isPresent()) {
            NotificationLog log = logOpt.get();
            
            if (log.getStatus() == NotificationLog.NotificationStatus.FAILED) {
                // Reset status to pending and retry
                log.setStatus(NotificationLog.NotificationStatus.RETRY);
                log.setErrorMessage(null);
                notificationLogRepository.save(log);
                
                // Retry sending the notification
                emailService.sendSimpleEmail(
                    log.getRecipientEmail(),
                    log.getSubject(),
                    log.getContent(),
                    log.getNotificationType(),
                    log.getUserId()
                );
            } else {
                throw new RuntimeException("Notification is not in failed status");
            }
        } else {
            throw new RuntimeException("Notification not found with id: " + notificationId);
        }
    }

    public void sendTestNotification(String email, String subject, String content) {
        emailService.sendSimpleEmail(
            email,
            subject,
            content,
            NotificationLog.NotificationType.PROMOTIONAL,
            null // No specific user ID for test notifications
        );
    }

    public void cleanupOldNotifications(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        
        // This would typically be implemented with a custom query
        // For now, we'll just log the cleanup operation
        System.out.println("Cleanup operation would remove notifications older than: " + cutoffDate);
    }

    public List<NotificationLog> getNotificationsByOrder(Long orderId) {
        return notificationLogRepository.findByOrderId(orderId);
    }

    public List<NotificationLog> getNotificationsByEmail(String email) {
        return notificationLogRepository.findByRecipientEmail(email);
    }

    public List<NotificationLog> getNotificationsByType(NotificationLog.NotificationType type) {
        return notificationLogRepository.findByNotificationType(type);
    }
}
