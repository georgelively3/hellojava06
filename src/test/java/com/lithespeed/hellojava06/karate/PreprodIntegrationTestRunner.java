package com.lithespeed.hellojava06.karate;

import com.intuit.karate.junit5.Karate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Preprod Test Runner - Uses real AWS S3 services
 * Requires proper AWS credentials and S3 bucket configuration
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("preprod")
public class PreprodIntegrationTestRunner {

    @LocalServerPort
    private int serverPort;

    @Karate.Test
    Karate testUsersInPreprod() {
        System.setProperty("karate.server.port", String.valueOf(serverPort));
        return Karate.run("user-api").relativeTo(getClass());
    }

    @Karate.Test
    Karate testS3InPreprod() {
        System.setProperty("karate.server.port", String.valueOf(serverPort));
        // No WireMock - uses real AWS S3
        return Karate.run("s3-api").relativeTo(getClass());
    }
}
