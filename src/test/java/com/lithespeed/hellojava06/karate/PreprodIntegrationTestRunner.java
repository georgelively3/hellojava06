package com.lithespeed.hellojava06.karate;

import com.intuit.karate.junit5.Karate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Preprod Test Runner - Uses LocalStack for S3 service virtualization
 * This allows testing without real AWS credentials
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "preprod", "localstack" })
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
        // Uses LocalStack for S3 virtualization
        return Karate.run("s3-api").relativeTo(getClass());
    }
}
