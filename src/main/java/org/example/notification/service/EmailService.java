package org.example.notification.service;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.*;
import org.example.notification.entity.NotificationLog;
import org.example.notification.repository.NotificationLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Service
public class EmailService {

    @Autowired(required = false)
    private AmazonSimpleEmailService amazonSESClient;
    
    @Autowired
    private JavaMailSender javaMailSender;
    
    @Autowired
    private TemplateEngine templateEngine;
    
    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Value("${notification.email.from:noreply@ecommerce.com}")
    private String fromEmail;

    @Value("${notification.email.provider:smtp}")
    private String emailProvider; // "ses" or "smtp"

    @Async
    public void sendSimpleEmail(String to, String subject, String content, 
                               NotificationLog.NotificationType notificationType, Long userId) {
        
        NotificationLog log = new NotificationLog(userId, to, subject, content, notificationType);
        log = notificationLogRepository.save(log);
        
        try {
            if ("ses".equalsIgnoreCase(emailProvider) && amazonSESClient != null) {
                sendEmailViaSES(to, subject, content);
            } else {
                sendEmailViaSMTP(to, subject, content);
            }
            
            log.markAsSent();
            System.out.println("Email sent successfully to: " + to + " - Subject: " + subject);
            
        } catch (Exception e) {
            log.markAsFailed(e.getMessage());
            System.err.println("Failed to send email to: " + to + " - Error: " + e.getMessage());
        } finally {
            notificationLogRepository.save(log);
        }
    }

    @Async
    public void sendTemplateEmail(String to, String subject, String templateName, 
                                 Map<String, Object> templateVariables, 
                                 NotificationLog.NotificationType notificationType, Long userId) {
        
        try {
            // Process template with Thymeleaf
            Context context = new Context();
            context.setVariables(templateVariables);
            String htmlContent = templateEngine.process(templateName, context);
            
            NotificationLog log = new NotificationLog(userId, to, subject, htmlContent, notificationType);
            log = notificationLogRepository.save(log);
            
            try {
                if ("ses".equalsIgnoreCase(emailProvider) && amazonSESClient != null) {
                    sendHtmlEmailViaSES(to, subject, htmlContent);
                } else {
                    sendHtmlEmailViaSMTP(to, subject, htmlContent);
                }
                
                log.markAsSent();
                System.out.println("Template email sent successfully to: " + to + " - Template: " + templateName);
                
            } catch (Exception e) {
                log.markAsFailed(e.getMessage());
                System.err.println("Failed to send template email to: " + to + " - Error: " + e.getMessage());
            } finally {
                notificationLogRepository.save(log);
            }
            
        } catch (Exception e) {
            System.err.println("Failed to process email template: " + templateName + " - Error: " + e.getMessage());
        }
    }

    private void sendEmailViaSES(String to, String subject, String content) {
        SendEmailRequest request = new SendEmailRequest()
                .withDestination(new Destination().withToAddresses(to))
                .withMessage(new Message()
                        .withBody(new Body().withText(new Content().withCharset("UTF-8").withData(content)))
                        .withSubject(new Content().withCharset("UTF-8").withData(subject)))
                .withSource(fromEmail);
        
        amazonSESClient.sendEmail(request);
    }

    private void sendHtmlEmailViaSES(String to, String subject, String htmlContent) {
        SendEmailRequest request = new SendEmailRequest()
                .withDestination(new Destination().withToAddresses(to))
                .withMessage(new Message()
                        .withBody(new Body()
                                .withHtml(new Content().withCharset("UTF-8").withData(htmlContent))
                                .withText(new Content().withCharset("UTF-8").withData(htmlContent)))
                        .withSubject(new Content().withCharset("UTF-8").withData(subject)))
                .withSource(fromEmail);
        
        amazonSESClient.sendEmail(request);
    }

    private void sendEmailViaSMTP(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        
        javaMailSender.send(message);
    }

    private void sendHtmlEmailViaSMTP(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true indicates HTML content
        
        javaMailSender.send(message);
    }

    // Convenience methods for specific notification types
    public void sendWelcomeEmail(String to, String firstName, Long userId) {
        Map<String, Object> variables = Map.of(
            "firstName", firstName,
            "companyName", "Ecommerce Platform"
        );
        
        sendTemplateEmail(to, "Welcome to Ecommerce Platform!", "welcome-email", 
                         variables, NotificationLog.NotificationType.USER_REGISTRATION, userId);
    }

    public void sendPasswordResetEmail(String to, String firstName, String resetToken, Long userId) {
        Map<String, Object> variables = Map.of(
            "firstName", firstName,
            "resetToken", resetToken,
            "resetUrl", "http://localhost:3000/reset-password?token=" + resetToken
        );
        
        sendTemplateEmail(to, "Password Reset Request", "password-reset-email", 
                         variables, NotificationLog.NotificationType.PASSWORD_RESET, userId);
    }

    public void sendOrderConfirmationEmail(String to, String firstName, String orderNumber, 
                                          String totalAmount, Long userId, Long orderId) {
        Map<String, Object> variables = Map.of(
            "firstName", firstName,
            "orderNumber", orderNumber,
            "totalAmount", totalAmount,
            "orderUrl", "http://localhost:3000/orders/" + orderNumber
        );
        
        NotificationLog log = new NotificationLog(userId, to, "Order Confirmation - " + orderNumber, 
                                                 "", NotificationLog.NotificationType.ORDER_CONFIRMATION);
        log.setOrderId(orderId);
        
        sendTemplateEmail(to, "Order Confirmation - " + orderNumber, "order-confirmation-email", 
                         variables, NotificationLog.NotificationType.ORDER_CONFIRMATION, userId);
    }

    public void sendPaymentSuccessEmail(String to, String firstName, String orderNumber, 
                                       String amount, Long userId, Long orderId) {
        Map<String, Object> variables = Map.of(
            "firstName", firstName,
            "orderNumber", orderNumber,
            "amount", amount
        );
        
        sendTemplateEmail(to, "Payment Successful - " + orderNumber, "payment-success-email", 
                         variables, NotificationLog.NotificationType.PAYMENT_SUCCESS, userId);
    }

    public void sendPaymentFailedEmail(String to, String firstName, String orderNumber, 
                                      String reason, Long userId, Long orderId) {
        Map<String, Object> variables = Map.of(
            "firstName", firstName,
            "orderNumber", orderNumber,
            "reason", reason
        );
        
        sendTemplateEmail(to, "Payment Failed - " + orderNumber, "payment-failed-email", 
                         variables, NotificationLog.NotificationType.PAYMENT_FAILED, userId);
    }
}
