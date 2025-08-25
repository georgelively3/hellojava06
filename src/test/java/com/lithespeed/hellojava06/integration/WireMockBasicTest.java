package com.lithespeed.hellojava06.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.lithespeed.hellojava06.config.WireMockS3Config;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;

/**
 * Simple test to verify WireMock setup works with our current dependencies.
 * Tests our S3 endpoints with S3Service configured for WireMock while
 * demonstrating WireMock capability.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("wiremock") // Use S3Service with WireMock configuration
@ContextConfiguration(classes = { WireMockS3Config.class })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WireMockBasicTest {

    @LocalServerPort
    private int port;

    private static WireMockServer wireMockServer;

    @BeforeAll
    static void setupWireMock() {
        try {
            // Start WireMock server
            wireMockServer = new WireMockServer(WireMockConfiguration.options()
                    .port(8089));
            wireMockServer.start();
            WireMock.configureFor("localhost", 8089);
            
            // Wait a moment for server to be ready
            Thread.sleep(100);
            
            System.out.println("WireMock server started on port 8089");
        } catch (Exception e) {
            System.err.println("Failed to start WireMock: " + e.getMessage());
            throw new RuntimeException("WireMock startup failed", e);
        }
    }

    @AfterAll
    static void tearDownWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
            System.out.println("WireMock server stopped");
        }
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();

        // Setup basic S3 API mocks that our S3Service will call
        // Mock S3 ListObjects API response for empty bucket
        stubFor(get(urlMatching("/test-bucket.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBody("""
                                <?xml version="1.0" encoding="UTF-8"?>
                                <ListObjectsV2Result xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
                                    <Name>test-bucket</Name>
                                    <Prefix></Prefix>
                                    <KeyCount>0</KeyCount>
                                    <MaxKeys>1000</MaxKeys>
                                    <IsTruncated>false</IsTruncated>
                                </ListObjectsV2Result>
                                """)));

        // Mock S3 PutObject API response
        stubFor(put(urlMatching("/test-bucket/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("ETag", "\"d41d8cd98f00b204e9800998ecf8427e\"")
                        .withHeader("x-amz-request-id", "mock-request-id")
                        .withHeader("x-amz-id-2", "mock-extended-request-id")));
    }

    @Test
    @Order(1)
    @DisplayName("S3 Health Check should work with WireMock running")
    void testS3HealthCheckWithWireMock() {
        // Test our actual S3 health endpoint (uses S3Service with WireMock)
        given()
                .port(port)
                .when()
                .get("/s3/health")
                .then()
                .statusCode(200);

        System.out.println("S3 Health check passed with WireMock running");
    }

    @Test
    @Order(2)
    @DisplayName("List files should work with S3Service while WireMock runs")
    void testListFilesWithWireMockRunning() {
        // Test our S3 list endpoint (uses S3Service with WireMock)
        given()
                .port(port)
                .when()
                .get("/s3/list")
                .then()
                .statusCode(200);

        // Verify WireMock received the request
        verify(getRequestedFor(urlMatching("/test-bucket.*")));
        System.out.println("S3 List files works with WireMock");
    }

    @Test
    @Order(3)
    @DisplayName("Upload file should work with S3Service while WireMock runs")
    void testFileUploadWithWireMockRunning() {
        // Test file upload endpoint (uses S3Service with WireMock)
        given()
                .port(port)
                .queryParam("fileName", "test-file.pdf")
                .when()
                .post("/s3/upload")
                .then()
                .statusCode(200);

        // Verify WireMock received the upload request
        verify(putRequestedFor(urlMatching("/test-bucket/test-file.pdf.*")));
        System.out.println("S3 File upload works with WireMock");
    }
}
