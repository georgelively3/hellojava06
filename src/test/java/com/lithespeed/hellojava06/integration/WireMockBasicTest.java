package com.lithespeed.hellojava06.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;

/**
 * Simple test to verify WireMock setup works with our current dependencies.
 * Tests our S3 endpoints with FakeS3Service while demonstrating WireMock
 * capability.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("fake-s3") // Use FakeS3Service to avoid AWS dependencies
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WireMockBasicTest {

    @LocalServerPort
    private int port;

    private static WireMockServer wireMockServer;

    @BeforeAll
    static void setupWireMock() {
        // Start WireMock server
        wireMockServer = new WireMockServer(WireMockConfiguration.options()
                .port(9999));
        wireMockServer.start();
        WireMock.configureFor("localhost", 9999);

        System.out.println("WireMock server started on port 9999");
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
    }

    @Test
    @Order(1)
    @DisplayName("S3 Health Check should work with WireMock running")
    void testS3HealthCheckWithWireMock() {
        // Setup a stub that we won't use, just to verify WireMock is working
        stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\":\"WireMock is working\"}")));

        // Test our actual S3 health endpoint (uses FakeS3Service)
        given()
                .port(port)
                .when()
                .get("/s3/health")
                .then()
                .statusCode(200);

        System.out.println("S3 Health check passed with WireMock running in background");
    }

    @Test
    @Order(2)
    @DisplayName("List files should work with FakeS3Service while WireMock runs")
    void testListFilesWithWireMockRunning() {
        // Test our S3 list endpoint (uses FakeS3Service)
        given()
                .port(port)
                .when()
                .get("/s3/list")
                .then()
                .statusCode(200);

        System.out.println("S3 List files works with WireMock running in background");
    }

    @Test
    @Order(3)
    @DisplayName("Upload file should work with FakeS3Service while WireMock runs")
    void testFileUploadWithWireMockRunning() {
        // Test file upload endpoint (uses FakeS3Service)
        given()
                .port(port)
                .queryParam("fileName", "test-file.pdf")
                .when()
                .post("/s3/upload")
                .then()
                .statusCode(200);

        System.out.println("S3 File upload works with WireMock running in background");
    }

    @Test
    @Order(4)
    @DisplayName("WireMock can be called directly from test")
    void testWireMockDirectly() {
        // Setup a mock endpoint
        stubFor(get(urlEqualTo("/mock-test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"mocked\",\"message\":\"Direct WireMock test successful\"}")));

        // Call WireMock directly
        given()
                .port(9999)
                .when()
                .get("/mock-test")
                .then()
                .statusCode(200);

        // Verify the call was made
        verify(getRequestedFor(urlEqualTo("/mock-test")));
        System.out.println("Direct WireMock call succeeded");
    }
}
