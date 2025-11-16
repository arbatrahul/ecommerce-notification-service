package org.example.notification.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class AwsConfig {

    @Value("${aws.accessKey:}")
    private String awsAccessKey;

    @Value("${aws.secretKey:}")
    private String awsSecretKey;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    @Profile("!test")
    public AmazonSimpleEmailService amazonSimpleEmailService() {
        // If AWS credentials are provided, use them
        if (!awsAccessKey.isEmpty() && !awsSecretKey.isEmpty()) {
            AWSCredentials awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
            return AmazonSimpleEmailServiceClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                    .withRegion(Regions.fromName(awsRegion))
                    .build();
        } else {
            // Use default credential provider chain (IAM roles, environment variables, etc.)
            return AmazonSimpleEmailServiceClientBuilder.standard()
                    .withRegion(Regions.fromName(awsRegion))
                    .build();
        }
    }
}
