package com.lithespeed.hellojava06.karate;

import com.intuit.karate.junit5.Karate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Karate Test Runner for API Integration Tests with Spring Boot integration
 * Replaces the Cucumber BDD approach with simplified Karate testing
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class KarateTestRunner {

    @LocalServerPort
    private int serverPort;

    @Karate.Test
    Karate testUsers() {
        System.setProperty("karate.server.port", String.valueOf(serverPort));
        return Karate.run("user-api").relativeTo(getClass());
    }

    @Karate.Test
    Karate testS3() {
        System.setProperty("karate.server.port", String.valueOf(serverPort));
        return Karate.run("s3-api").relativeTo(getClass());
    }
}
