package com.lithespeed.hellojava06.karate;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.intuit.karate.junit5.Karate;
import com.lithespeed.hellojava06.config.WireMockS3Config;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Karate Test Runner with WireMock S3 service virtualization
 * Uses real S3Service implementation against mocked S3 responses
 * 
 * Consolidated test runner that includes:
 * - User API tests
 * - S3 API tests (with WireMock S3 stubs)
 * - Dialog API tests (from existing DialogTest)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "wiremock", "test" })
@Import(WireMockS3Config.class)
public class KarateTestRunner {

    @LocalServerPort
    private int serverPort;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(8089))
            .build();

    @BeforeAll
    static void setupS3Stubs() {
        // Mock S3 PUT (upload) operations - return success for any file
        wireMock.stubFor(put(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("ETag", "\"test-etag-12345\"")
                        .withBody("")));

        // Mock S3 LIST operations - return XML list based on what's been uploaded
        // For empty bucket
        wireMock.stubFor(get(urlMatching(".*\\?list-type=2"))
                .withQueryParam("list-type", equalTo("2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<ListBucketResult>" +
                                "<Name>test-bucket</Name>" +
                                "<Contents/>" +
                                "</ListBucketResult>")));

        // Mock for populated bucket - this will be enhanced based on upload tracking
        wireMock.stubFor(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<ListBucketResult>" +
                                "<Name>test-bucket</Name>" +
                                "<Contents>" +
                                "<Key>karate-test.txt</Key>" +
                                "</Contents>" +
                                "<Contents>" +
                                "<Key>file1.txt</Key>" +
                                "</Contents>" +
                                "<Contents>" +
                                "<Key>file2.txt</Key>" +
                                "</Contents>" +
                                "<Contents>" +
                                "<Key>file3.txt</Key>" +
                                "</Contents>" +
                                "</ListBucketResult>")));

        System.out.println("WireMock S3 stubs configured on port: " + wireMock.getPort());
    }

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

    @Karate.Test
    public Karate testDialogs() {
        System.setProperty("karate.server.port", String.valueOf(serverPort));
        return Karate.run("classpath:features/karate/dialog.feature");
    }
}
