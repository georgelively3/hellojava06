package com.lithespeed.hellojava06.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Composite annotation for Spring Boot tests that includes all necessary
 * configuration
 * to avoid dependency injection issues with S3 and other enterprise components.
 * 
 * Usage:
 * 
 * @IntegrationTestConfiguration
 *                               class YourExistingTest {
 *                               // Your existing test code
 *                               }
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = { TestS3Config.class })
@TestPropertySource(properties = {
        "aws.s3.bucket-name=test-bucket",
        "aws.s3.region=us-east-1",
        "aws.s3.access-key=test",
        "aws.s3.secret-key=test",
        "aws.s3.endpoint-url=http://localhost:4566",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "logging.level.com.lithespeed.hellojava06=DEBUG"
})
public @interface IntegrationTestConfiguration {
}
