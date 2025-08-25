package com.lithespeed.hellojava06.integration;

import com.lithespeed.hellojava06.extension.S3WireMockExtension;
import com.lithespeed.hellojava06.config.WireMockS3Config;
import com.lithespeed.hellojava06.service.S3Service;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;

/**
 * Simple test to verify WireMock setup works with our current dependencies.
 * Tests our S3 endpoints with S3Service configured for WireMock while
 * demonstrating WireMock capability using the S3WireMockExtension.
 */
@ExtendWith(S3WireMockExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("wiremock") // Use S3Service with WireMock configuration
@ContextConfiguration(classes = { WireMockS3Config.class })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WireMockBasicTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext context;
    
    @Autowired
    private Environment environment;

    @BeforeEach
    void resetWireMock() {
        // Reset WireMock state before each test (mocks are set up by extension)
        reset();
        
        // Debug info
        System.out.println("Active profiles: " + java.util.Arrays.toString(environment.getActiveProfiles()));
        System.out.println("Environment bean count: " + context.getBeanDefinitionCount());
        
        // Check if we have the correct S3Service bean
        try {
            S3Service s3Service = context.getBean(S3Service.class);
            System.out.println("S3Service bean class: " + s3Service.getClass().getName());
        } catch (Exception e) {
            System.out.println("Error getting S3Service bean: " + e.getMessage());
        }
        
        // Test WireMock is accessible
        try {
            var response = java.net.http.HttpClient.newHttpClient()
                    .send(java.net.http.HttpRequest.newBuilder()
                            .uri(java.net.URI.create("http://localhost:9090/__admin/"))
                            .build(), java.net.http.HttpResponse.BodyHandlers.ofString());
            System.out.println("WireMock admin response: " + response.statusCode());
        } catch (Exception e) {
            System.err.println("WireMock admin not accessible: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("S3 Health Check should work with WireMock running")
    void testS3HealthCheckWithWireMock() {
        System.out.println("Testing S3 health check endpoint on port: " + port);
        
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
        System.out.println("Testing S3 list files endpoint on port: " + port);
        
        // Test our S3 list endpoint (uses S3Service with WireMock)
        var response = given()
                .port(port)
                .when()
                .get("/s3/list");
        
        System.out.println("Response status: " + response.statusCode());
        System.out.println("Full response body: " + response.body().asString());
        System.out.println("Response headers: " + response.headers());
        
        if (response.statusCode() == 500) {
            System.err.println("500 Error detected - this suggests S3Service is not connecting to WireMock properly");
        }

        response.then().statusCode(200);

        // For now, just verify the endpoint works - detailed WireMock verification can be added later
        System.out.println("S3 List files works with WireMock");
    }

    @Test
    @Order(3)
    @DisplayName("Upload file should work with S3Service while WireMock runs")
    void testFileUploadWithWireMockRunning() {
        System.out.println("Testing S3 upload endpoint on port: " + port);
        
        // Test file upload endpoint (uses S3Service with WireMock)
        var response = given()
                .port(port)
                .queryParam("fileName", "test-file.pdf")
                .when()
                .post("/s3/upload");
        
        System.out.println("Upload response status: " + response.statusCode());
        if (response.statusCode() != 200) {
            System.out.println("Upload response body: " + response.body().asString());
        }

        response.then().statusCode(200);

        // For now, just verify the endpoint works - detailed WireMock verification can be added later
        System.out.println("S3 File upload works with WireMock");
    }
}
