package com.lithespeed.hellojava06.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock configuration for service virtualization in integration tests
 * Provides mock external services to eliminate dependencies during testing
 */
@TestConfiguration
@Profile("wiremock")
public class WireMockConfig {

    @Value("${wiremock.port:9999}")
    private int wireMockPort;

    @Bean
    public WireMockServer wireMockServer() {
        WireMockServer wireMockServer = new WireMockServer(
                WireMockConfiguration.wireMockConfig()
                        .port(wireMockPort)
                        .usingFilesUnderClasspath("wiremock"));

        // Start the server
        wireMockServer.start();

        // Configure global settings
        configureFor("localhost", wireMockPort);

        // Setup default stubs for external services
        setupDefaultStubs(wireMockServer);

        return wireMockServer;
    }

    private void setupDefaultStubs(WireMockServer wireMockServer) {
        // Example: Mock external API responses
        wireMockServer.stubFor(get(urlEqualTo("/external/health"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"UP\", \"source\": \"wiremock\"}")));

        // Example: Mock S3-like service (if needed for testing external S3 calls)
        wireMockServer.stubFor(put(urlMatching("/mock-s3/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"uploaded\": true, \"source\": \"wiremock-s3\"}")));

        wireMockServer.stubFor(get(urlEqualTo("/mock-s3/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                "{\"files\": [\"mock-file-1.txt\", \"mock-file-2.txt\"], \"source\": \"wiremock-s3\"}")));

        System.out.println("WireMock server started on port " + wireMockPort + " with default stubs");
    }
}
