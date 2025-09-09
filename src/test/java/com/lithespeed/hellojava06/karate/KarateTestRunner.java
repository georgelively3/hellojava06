package com.lithespeed.hellojava06.karate;

import com.intuit.karate.junit5.Karate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Karate test runner for S3 feature scenarios
 * Runs all scenarios - use gradle tags to control which ones execute
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.autoconfigure.exclude=io.awspring.cloud.autoconfigure.s3.S3AutoConfiguration,io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration"
    }
)
public class KarateTestRunner {

    @LocalServerPort
    private int serverPort;

    @Karate.Test
    Karate testS3Scenarios() {
        System.setProperty("karate.server.port", String.valueOf(serverPort));
        return Karate.run("classpath:karate/s3.feature");
        // Note: Use gradle properties to control which scenarios run:
        // Normal: ./gradlew test (runs non-@preprod scenarios)  
        // Preprod: ./gradlew test -Ppreprod (runs @preprod scenarios)
    }
}