FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/notification-service-1.0-SNAPSHOT.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]
