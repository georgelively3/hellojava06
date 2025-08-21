package com.lithespeed.hellojava06.karate;

import com.intuit.karate.junit5.Karate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration Test Runner for DEV/INT environments with mock S3 service
 * Uses fake-s3 profile to avoid real S3 dependencies during development testing
 * 
 * Note: WireMock integration can be added later if needed for external service
 * mocking
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("fake-s3")
public class DevIntegrationTestRunner {

    @LocalServerPort
    private int serverPort;

    @BeforeAll
    static void setup() {
        System.out.println("DEV Integration Tests - Using FakeS3Service for S3 operations");
    }

    @AfterAll
    static void tearDown() {
        System.out.println("DEV Integration Tests completed");
    }

    @Karate.Test
    Karate testUsersInDev() {
        System.setProperty("karate.server.port", String.valueOf(serverPort));
        return Karate.run("user-api").relativeTo(getClass());
    }

    @Karate.Test
    Karate testS3InDev() {
        System.setProperty("karate.server.port", String.valueOf(serverPort));
        return Karate.run("s3-api").relativeTo(getClass());
    }
}
