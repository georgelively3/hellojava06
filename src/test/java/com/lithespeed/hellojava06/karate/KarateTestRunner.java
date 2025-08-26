package com.lithespeed.hellojava06.karate;

import com.intuit.karate.junit5.Karate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Consolidated Karate test runner for all feature files
 * Simple health check test to validate setup
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class KarateTestRunner {

    @LocalServerPort
    private int serverPort;

    @Karate.Test
    Karate testS3Health() {
        System.setProperty("karate.server.port", String.valueOf(serverPort));
        return Karate.run("classpath:karate/s3.feature");
    }
}