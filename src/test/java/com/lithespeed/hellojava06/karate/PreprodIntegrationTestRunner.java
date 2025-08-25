package com.lithespeed.hellojava06.karate;

import com.intuit.karate.junit5.Karate;
import com.lithespeed.hellojava06.config.WireMockS3Config;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Preprod Test Runner - Uses WireMock for S3 service virtualization
 * This allows testing without real AWS credentials
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "preprod", "wiremock" })
@ContextConfiguration(classes = { WireMockS3Config.class })
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
