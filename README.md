# Ecommerce Notification Service

Microservice for sending email and SMS notifications to users in the Ecommerce Platform.

## Overview

The Notification Service handles all user notifications including:
- Order confirmation emails
- Password reset emails
- Welcome emails
- Order status updates
- Payment confirmations
- Custom notifications

## Features

- ✅ **Email Notifications**: SMTP and AWS SES support
- ✅ **Template Engine**: Thymeleaf for HTML email templates
- ✅ **Kafka Integration**: Event-driven notification processing
- ✅ **Notification Logging**: Track all sent notifications
- ✅ **Retry Mechanism**: Retry failed notifications
- ✅ **Statistics**: Notification analytics
- ✅ **Service Discovery**: Eureka client integration
- ✅ **MySQL Database**: Persistent notification logs
- ✅ **OpenFeign**: Service-to-service communication

## Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- MySQL 8.0+ (Port 3310)
- Kafka (Port 9092)
- Eureka Server (Port 8761)
- AWS SES (optional, for production) or SMTP server

### Database Setup

1. **Create MySQL Database**:
   ```sql
   CREATE DATABASE notification_service_db;
   ```

2. **Configure Email Provider**:
   - **SMTP**: Configure Gmail or other SMTP server
   - **AWS SES**: Configure AWS credentials

### Running Locally

1. **Build the project**:
   ```bash
   mvn clean package
   ```

2. **Configure Email Settings**:
   ```bash
   export MAIL_USERNAME=your-email@gmail.com
   export MAIL_PASSWORD=your-app-password
   export EMAIL_PROVIDER=smtp  # or "ses"
   ```

3. **Run the application**:
   ```bash
   java -jar target/notification-service-1.0.0.jar
   ```

4. **Or use Maven**:
   ```bash
   mvn spring-boot:run
   ```

The service will start on `http://localhost:8086`

## API Endpoints

### Notification Endpoints

#### Get User Notifications
```http
GET /api/notifications/user/{userId}?page=0&size=20
```

#### Get Notification Statistics
```http
GET /api/notifications/stats
```

#### Get Failed Notifications (Admin)
```http
GET /api/notifications/failed
```

#### Retry Failed Notification (Admin)
```http
POST /api/notifications/{id}/retry
```

#### Send Test Notification (Admin)
```http
POST /api/notifications/test?email=test@example.com&subject=Test&content=Test message
```

## Configuration

### Application Configuration (application.yml)

```yaml
server:
  port: 8086

spring:
  application:
    name: notification-service
  datasource:
    url: jdbc:mysql://localhost:3310/notification_service_db
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
  kafka:
    bootstrap-servers: localhost:9092
```

### Email Provider Configuration

#### SMTP Configuration
```yaml
notification:
  email:
    provider: smtp
    from: noreply@ecommerce.com
```

#### AWS SES Configuration
```yaml
aws:
  accessKey: ${AWS_ACCESS_KEY}
  secretKey: ${AWS_SECRET_KEY}
  region: ${AWS_REGION:us-east-1}

notification:
  email:
    provider: ses
    from: noreply@ecommerce.com
```

### Email Templates

Templates are located in `src/main/resources/templates/`:
- `welcome-email.html` - Welcome email template
- `order-confirmation-email.html` - Order confirmation template
- `password-reset-email.html` - Password reset template

## Usage Examples

### Get User Notifications

```bash
curl "http://localhost:8086/api/notifications/user/1?page=0&size=20"
```

### Get Notification Statistics

```bash
curl http://localhost:8086/api/notifications/stats
```

### Send Test Notification

```bash
curl -X POST "http://localhost:8086/api/notifications/test?email=test@example.com&subject=Test&content=Test%20message"
```

### Retry Failed Notification

```bash
curl -X POST http://localhost:8086/api/notifications/1/retry
```

## Architecture

### Notification Flow

1. **Event Received** → Kafka Consumer receives notification event
2. **Template Selection** → Select appropriate email template
3. **User Data Fetch** → Fetch user details via OpenFeign
4. **Email Composition** → Compose email with template and data
5. **Email Sending** → Send via SMTP or AWS SES
6. **Logging** → Log notification to database
7. **Retry Logic** → Retry on failure

### Components

1. **NotificationController**: REST endpoints
2. **NotificationService**: Business logic
3. **EmailService**: Email sending (SMTP/SES)
4. **NotificationEventConsumer**: Kafka consumer
5. **UserServiceClient**: OpenFeign client for user service
6. **NotificationLogRepository**: Database operations

## Kafka Integration

### Topics Consumed

- `order-events`: Order creation and status updates
- `user-events`: User registration and password reset
- `payment-events`: Payment confirmations

### Event Types

- `ORDER_CREATED` → Order confirmation email
- `ORDER_STATUS_UPDATED` → Order status update email
- `USER_REGISTERED` → Welcome email
- `PASSWORD_RESET` → Password reset email
- `PAYMENT_CONFIRMED` → Payment confirmation email

## Email Templates

### Welcome Email Template

Located at: `src/main/resources/templates/welcome-email.html`

Variables:
- `${userName}` - User's name
- `${userEmail}` - User's email

### Order Confirmation Email Template

Located at: `src/main/resources/templates/order-confirmation-email.html`

Variables:
- `${orderNumber}` - Order number
- `${orderDate}` - Order date
- `${orderItems}` - List of order items
- `${totalAmount}` - Total order amount
- `${shippingAddress}` - Shipping address

### Password Reset Email Template

Located at: `src/main/resources/templates/password-reset-email.html`

Variables:
- `${userName}` - User's name
- `${resetLink}` - Password reset link
- `${expirationTime}` - Link expiration time

## Testing

### Run Tests

```bash
mvn test
```

### Manual Testing

1. Start MySQL, Kafka, Eureka
2. Start Notification Service
3. Send test notification via API
4. Check email inbox
5. Verify notification logs in database

### Test Email Configuration

For local testing with Gmail:

1. Enable 2-factor authentication
2. Generate app password
3. Use app password in `MAIL_PASSWORD`

## Deployment

### Docker

```bash
# Build image
docker build -t ecommerce/notification-service .

# Run container
docker run -p 8086:8086 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e MAIL_USERNAME=your-email@gmail.com \
  -e MAIL_PASSWORD=your-app-password \
  -e EMAIL_PROVIDER=smtp \
  ecommerce/notification-service
```

### Production Considerations

1. **Email Provider**:
   - Use AWS SES for production
   - Configure verified sender domains
   - Set up bounce and complaint handling
2. **Database**:
   - Archive old notification logs
   - Set up proper indexing
3. **Kafka**:
   - Configure proper consumer groups
   - Handle message failures
4. **Monitoring**:
   - Monitor email delivery rates
   - Track failed notifications
   - Set up alerts for high failure rates

## Troubleshooting

### Common Issues

1. **Email Not Sending (SMTP)**:
   - Verify SMTP credentials
   - Check firewall settings
   - Verify port 587 is open
   - Check Gmail app password

2. **Email Not Sending (AWS SES)**:
   - Verify AWS credentials
   - Check SES region configuration
   - Verify sender email is verified
   - Check SES sending limits

3. **Templates Not Found**:
   - Verify template files exist
   - Check template path configuration
   - Verify Thymeleaf configuration

4. **Kafka Consumer Not Receiving Events**:
   - Verify Kafka connection
   - Check consumer group configuration
   - Verify topic exists

### Logs

```bash
# Enable debug logging
java -jar target/notification-service-1.0.0.jar \
  --logging.level.org.example.notification=DEBUG \
  --logging.level.com.amazonaws=DEBUG
```

## Dependencies

- Spring Boot 3.2.0
- Spring Data JPA (MySQL)
- Spring Mail
- Spring Kafka
- Thymeleaf
- AWS SDK for SES
- Spring Cloud OpenFeign
- Spring Cloud Netflix Eureka Client
- MySQL Connector

## Project Structure

```
src/
├── main/
│   ├── java/org/example/notification/
│   │   ├── NotificationServiceApplication.java
│   │   ├── controller/
│   │   │   └── NotificationController.java
│   │   ├── service/
│   │   │   ├── NotificationService.java
│   │   │   └── EmailService.java
│   │   ├── consumer/
│   │   │   └── NotificationEventConsumer.java
│   │   ├── entity/
│   │   │   └── NotificationLog.java
│   │   ├── repository/
│   │   │   └── NotificationLogRepository.java
│   │   ├── client/
│   │   │   └── UserServiceClient.java
│   │   ├── dto/
│   │   │   └── UserDto.java
│   │   └── config/
│   │       └── AwsConfig.java
│   └── resources/
│       ├── application.yml
│       └── templates/
│           ├── welcome-email.html
│           ├── order-confirmation-email.html
│           └── password-reset-email.html
└── test/
```

## Contributing

1. Follow Spring Boot best practices
2. Write comprehensive tests
3. Update email templates when needed
4. Document notification event types
5. Handle errors gracefully

## License

This project is part of the Ecommerce Microservices Platform.
