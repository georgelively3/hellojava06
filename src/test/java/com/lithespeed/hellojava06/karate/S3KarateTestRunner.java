package com.lithespeed.hellojava06.karate;

import com.intuit.karate.junit5.Karate;
import com.lithespeed.hellojava06.extension.S3WireMockExtension;
import com.lithespeed.hellojava06.config.WireMockS3Config;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * S3-focused Karate test runner using real S3Service with WireMock
 * for comprehensive API testing
 */
@ExtendWith(S3WireMockExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("wiremock")
@ContextConfiguration(classes = { WireMockS3Config.class })
public class S3KarateTestRunner {

    @LocalServerPort
    private int serverPort;

    @Karate.Test
    Karate testS3Api() {
        System.setProperty("karate.server.port", String.valueOf(serverPort));
        return Karate.run("s3-api").relativeTo(getClass());
    }
}
