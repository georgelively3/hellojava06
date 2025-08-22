package com.lithespeed.hellojava06.karate;

import com.intuit.karate.junit5.Karate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Focused Karate Test Runner for S3 API only
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test") // Uses fake-s3 profile which loads FakeS3Service
public class S3KarateTestRunner {

    @LocalServerPort
    private int serverPort;

    @Karate.Test
    Karate testS3Api() {
        System.setProperty("karate.server.port", String.valueOf(serverPort));
        return Karate.run("s3-api").relativeTo(getClass());
    }
}
