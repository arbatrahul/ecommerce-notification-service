package org.example.notification.consumer;

import org.example.notification.client.UserServiceClient;
import org.example.notification.dto.UserDto;
import org.example.notification.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationEventConsumer {

    @Autowired
    private EmailService emailService;
    
    @Autowired
    private UserServiceClient userServiceClient;

    @KafkaListener(topics = "user-events", groupId = "notification-service-group")
    public void handleUserEvent(@Payload Map<String, Object> userEvent, 
                               @Header(KafkaHeaders.KEY) String eventType) {
        
        try {
            String eventTypeValue = userEvent.get("eventType").toString();
            Long userId = Long.valueOf(userEvent.get("userId").toString());
            String email = userEvent.get("email").toString();
            
            switch (eventTypeValue) {
                case "USER_REGISTERED":
                    handleUserRegistered(userId, email);
                    break;
                case "PASSWORD_CHANGED":
                    handlePasswordChanged(userId, email);
                    break;
                default:
                    System.out.println("Unhandled user event type: " + eventTypeValue);
            }
        } catch (Exception e) {
            System.err.println("Error processing user event: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "notification-events", groupId = "notification-service-group")
    public void handleNotificationEvent(@Payload Map<String, Object> notificationEvent, 
                                       @Header(KafkaHeaders.KEY) String eventType) {
        
        try {
            String eventTypeValue = eventType;
            
            switch (eventTypeValue) {
                case "password-reset-requested":
                    handlePasswordResetRequested(notificationEvent);
                    break;
                case "order-payment-success":
                    handleOrderPaymentSuccess(notificationEvent);
                    break;
                case "order-payment-failed":
                    handleOrderPaymentFailed(notificationEvent);
                    break;
                case "order-refunded":
                    handleOrderRefunded(notificationEvent);
                    break;
                default:
                    System.out.println("Unhandled notification event type: " + eventTypeValue);
            }
        } catch (Exception e) {
            System.err.println("Error processing notification event: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "order-events", groupId = "notification-service-group")
    public void handleOrderEvent(@Payload Map<String, Object> orderEvent, 
                                @Header(KafkaHeaders.KEY) String eventType) {
        
        try {
            String eventTypeValue = orderEvent.get("eventType").toString();
            Long userId = Long.valueOf(orderEvent.get("userId").toString());
            Long orderId = Long.valueOf(orderEvent.get("orderId").toString());
            
            switch (eventTypeValue) {
                case "ORDER_CREATED":
                    handleOrderCreated(userId, orderId, orderEvent);
                    break;
                case "ORDER_STATUS_UPDATED":
                    handleOrderStatusUpdated(userId, orderId, orderEvent);
                    break;
                default:
                    System.out.println("Unhandled order event type: " + eventTypeValue);
            }
        } catch (Exception e) {
            System.err.println("Error processing order event: " + e.getMessage());
        }
    }

    private void handleUserRegistered(Long userId, String email) {
        try {
            // Get user details from User Service
            UserDto user = userServiceClient.getUserById(userId);
            if (user != null) {
                emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName(), userId);
                System.out.println("Welcome email sent to: " + user.getEmail());
            }
        } catch (Exception e) {
            System.err.println("Failed to send welcome email for user: " + userId + " - " + e.getMessage());
        }
    }

    private void handlePasswordChanged(Long userId, String email) {
        try {
            UserDto user = userServiceClient.getUserById(userId);
            if (user != null) {
                emailService.sendSimpleEmail(
                    user.getEmail(),
                    "Password Changed Successfully",
                    "Hello " + user.getFirstName() + ",\n\n" +
                    "Your password has been changed successfully. If you did not make this change, " +
                    "please contact our support team immediately.\n\n" +
                    "Best regards,\nEcommerce Platform Team",
                    org.example.notification.entity.NotificationLog.NotificationType.PASSWORD_RESET,
                    userId
                );
                System.out.println("Password change notification sent to: " + user.getEmail());
            }
        } catch (Exception e) {
            System.err.println("Failed to send password change notification for user: " + userId + " - " + e.getMessage());
        }
    }

    private void handlePasswordResetRequested(Map<String, Object> event) {
        try {
            Long userId = Long.valueOf(event.get("userId").toString());
            String email = event.get("email").toString();
            String resetToken = event.get("resetToken").toString();
            
            UserDto user = userServiceClient.getUserById(userId);
            if (user != null) {
                emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetToken, userId);
                System.out.println("Password reset email sent to: " + user.getEmail());
            }
        } catch (Exception e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
        }
    }

    private void handleOrderCreated(Long userId, Long orderId, Map<String, Object> event) {
        try {
            UserDto user = userServiceClient.getUserById(userId);
            if (user != null) {
                String amount = event.get("amount").toString();
                String orderNumber = "ORD-" + orderId; // Simplified order number
                
                emailService.sendOrderConfirmationEmail(
                    user.getEmail(), 
                    user.getFirstName(), 
                    orderNumber, 
                    "$" + amount, 
                    userId, 
                    orderId
                );
                System.out.println("Order confirmation email sent to: " + user.getEmail());
            }
        } catch (Exception e) {
            System.err.println("Failed to send order confirmation email: " + e.getMessage());
        }
    }

    private void handleOrderStatusUpdated(Long userId, Long orderId, Map<String, Object> event) {
        try {
            UserDto user = userServiceClient.getUserById(userId);
            if (user != null) {
                String orderNumber = "ORD-" + orderId;
                
                emailService.sendSimpleEmail(
                    user.getEmail(),
                    "Order Status Update - " + orderNumber,
                    "Hello " + user.getFirstName() + ",\n\n" +
                    "Your order " + orderNumber + " status has been updated.\n\n" +
                    "You can track your order at: http://localhost:3000/orders/" + orderNumber + "\n\n" +
                    "Best regards,\nEcommerce Platform Team",
                    org.example.notification.entity.NotificationLog.NotificationType.ORDER_CONFIRMATION,
                    userId
                );
                System.out.println("Order status update email sent to: " + user.getEmail());
            }
        } catch (Exception e) {
            System.err.println("Failed to send order status update email: " + e.getMessage());
        }
    }

    private void handleOrderPaymentSuccess(Map<String, Object> event) {
        try {
            Long userId = Long.valueOf(event.get("userId").toString());
            Long orderId = Long.valueOf(event.get("orderId").toString());
            
            UserDto user = userServiceClient.getUserById(userId);
            if (user != null) {
                String orderNumber = "ORD-" + orderId;
                
                emailService.sendSimpleEmail(
                    user.getEmail(),
                    "Payment Successful - " + orderNumber,
                    "Hello " + user.getFirstName() + ",\n\n" +
                    "Your payment for order " + orderNumber + " has been processed successfully.\n\n" +
                    "Thank you for your purchase!\n\n" +
                    "Best regards,\nEcommerce Platform Team",
                    org.example.notification.entity.NotificationLog.NotificationType.PAYMENT_SUCCESS,
                    userId
                );
                System.out.println("Payment success email sent to: " + user.getEmail());
            }
        } catch (Exception e) {
            System.err.println("Failed to send payment success email: " + e.getMessage());
        }
    }

    private void handleOrderPaymentFailed(Map<String, Object> event) {
        try {
            Long userId = Long.valueOf(event.get("userId").toString());
            Long orderId = Long.valueOf(event.get("orderId").toString());
            
            UserDto user = userServiceClient.getUserById(userId);
            if (user != null) {
                String orderNumber = "ORD-" + orderId;
                
                emailService.sendSimpleEmail(
                    user.getEmail(),
                    "Payment Failed - " + orderNumber,
                    "Hello " + user.getFirstName() + ",\n\n" +
                    "Unfortunately, your payment for order " + orderNumber + " could not be processed.\n\n" +
                    "Please try again or contact our support team for assistance.\n\n" +
                    "Best regards,\nEcommerce Platform Team",
                    org.example.notification.entity.NotificationLog.NotificationType.PAYMENT_FAILED,
                    userId
                );
                System.out.println("Payment failed email sent to: " + user.getEmail());
            }
        } catch (Exception e) {
            System.err.println("Failed to send payment failed email: " + e.getMessage());
        }
    }

    private void handleOrderRefunded(Map<String, Object> event) {
        try {
            Long userId = Long.valueOf(event.get("userId").toString());
            Long orderId = Long.valueOf(event.get("orderId").toString());
            
            UserDto user = userServiceClient.getUserById(userId);
            if (user != null) {
                String orderNumber = "ORD-" + orderId;
                
                emailService.sendSimpleEmail(
                    user.getEmail(),
                    "Refund Processed - " + orderNumber,
                    "Hello " + user.getFirstName() + ",\n\n" +
                    "Your refund for order " + orderNumber + " has been processed successfully.\n\n" +
                    "The refund amount will be credited to your original payment method within 3-5 business days.\n\n" +
                    "Best regards,\nEcommerce Platform Team",
                    org.example.notification.entity.NotificationLog.NotificationType.ORDER_CANCELLED,
                    userId
                );
                System.out.println("Refund notification email sent to: " + user.getEmail());
            }
        } catch (Exception e) {
            System.err.println("Failed to send refund notification email: " + e.getMessage());
        }
    }
}
