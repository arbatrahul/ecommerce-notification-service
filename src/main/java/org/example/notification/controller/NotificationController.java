package org.example.notification.controller;

import org.example.notification.entity.NotificationLog;
import org.example.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // Get notification logs for a user
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationLog> notificationPage = notificationService.getUserNotifications(userId, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("notifications", notificationPage.getContent());
        response.put("currentPage", notificationPage.getNumber());
        response.put("totalItems", notificationPage.getTotalElements());
        response.put("totalPages", notificationPage.getTotalPages());
        response.put("hasNext", notificationPage.hasNext());
        response.put("hasPrevious", notificationPage.hasPrevious());
        
        return ResponseEntity.ok(response);
    }

    // Get notification statistics
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getNotificationStats() {
        Map<String, Object> stats = notificationService.getNotificationStatistics();
        return ResponseEntity.ok(stats);
    }

    // Get failed notifications (Admin)
    @GetMapping("/failed")
    public ResponseEntity<List<NotificationLog>> getFailedNotifications() {
        List<NotificationLog> failedNotifications = notificationService.getFailedNotifications();
        return ResponseEntity.ok(failedNotifications);
    }

    // Retry failed notification (Admin)
    @PostMapping("/{id}/retry")
    public ResponseEntity<Map<String, Object>> retryNotification(@PathVariable Long id) {
        try {
            notificationService.retryNotification(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification retry initiated");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Send test notification (Admin)
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> sendTestNotification(
            @RequestParam String email,
            @RequestParam String subject,
            @RequestParam String content) {
        
        try {
            notificationService.sendTestNotification(email, subject, content);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test notification sent successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
}
