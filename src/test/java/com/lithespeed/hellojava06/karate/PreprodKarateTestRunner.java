package com.lithespeed.hellojava06.karate;

import com.intuit.karate.junit5.Karate;
import com.lithespeed.hellojava06.config.AwsConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

/**
 * Preprod integration test runner
 * Tests the application with preprod profile configuration
 * Uses real AWS S3 bucket: pm3547b
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("preprod")
@Import(AwsConfig.class)
public class PreprodKarateTestRunner {

    @LocalServerPort
    private int serverPort;

    @Karate.Test
    Karate testS3HealthInPreprod() {
        System.setProperty("karate.server.port", String.valueOf(serverPort));
        return Karate.run("classpath:karate/s3.feature")
                .tags("~@preprod"); // Only run non-preprod scenarios (health check)
    }
    
    @Karate.Test
    Karate testS3IntegrationInPreprod() {
        System.setProperty("karate.server.port", String.valueOf(serverPort));
        return Karate.run("classpath:karate/s3.feature")
                .tags("@preprod"); // Only run preprod-tagged scenarios (file operations)
    }
}
