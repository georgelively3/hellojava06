package com.lithespeed.hellojava06.karate;

import com.intuit.karate.junit5.Karate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Integration Test Runner with WireMock service virtualization
 * Uses WireMock to mock external services while keeping FakeS3Service for S3
 * operations
 * 
 * This follows your org's proven integration testing patterns while adding
 * service virtualization
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "fake-s3", "wiremock" })
public class WireMockIntegrationTestRunner {

    @LocalServerPort
    private int serverPort;

    private static WireMockServer wireMockServer;
    private static final int WIREMOCK_PORT = 9999;

    @BeforeAll
    static void setupWireMock() {
        // Start WireMock server for external service mocking
        wireMockServer = new WireMockServer(
                WireMockConfiguration.wireMockConfig()
                        .port(WIREMOCK_PORT)
                        .usingFilesUnderClasspath("wiremock"));

        wireMockServer.start();
        configureFor("localhost", WIREMOCK_PORT);

        // Setup mock responses for external services
        setupMockResponses();

        System.out.println("WireMock Integration Tests - Using FakeS3Service + WireMock external service mocking");
        System.out.println("WireMock server running on port: " + WIREMOCK_PORT);
    }

    @AfterAll
    static void tearDownWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
            System.out.println("WireMock Integration Tests completed - server stopped");
        }
    }

    private static void setupMockResponses() {
        // Example: Mock external health check service
        wireMockServer.stubFor(get(urlEqualTo("/external/health"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                "{\"status\": \"UP\", \"service\": \"external-mock\", \"timestamp\": \"2025-08-21T16:00:00\"}")));

        // Example: Mock external API that your app might call
        wireMockServer.stubFor(post(urlEqualTo("/external/api/notify"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"notified\": true, \"messageId\": \"mock-12345\"}")));

        // Example: Mock file processing service
        wireMockServer.stubFor(post(urlMatching("/external/process/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"processed\": true, \"status\": \"completed\"}")));

        System.out.println("WireMock stubs configured for external service mocking");
    }

    @Karate.Test
    Karate testUsersWithWireMock() {
        System.setProperty("karate.server.port", String.valueOf(serverPort));
        System.setProperty("wiremock.port", String.valueOf(WIREMOCK_PORT));
        return Karate.run("user-api").relativeTo(getClass());
    }

    @Karate.Test
    Karate testS3WithWireMock() {
        System.setProperty("karate.server.port", String.valueOf(serverPort));
        System.setProperty("wiremock.port", String.valueOf(WIREMOCK_PORT));
        return Karate.run("s3-api").relativeTo(getClass());
    }

    @Karate.Test
    Karate testExternalIntegrations() {
        System.setProperty("karate.server.port", String.valueOf(serverPort));
        System.setProperty("wiremock.port", String.valueOf(WIREMOCK_PORT));
        return Karate.run("external-api").relativeTo(getClass());
    }
}
