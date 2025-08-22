package com.lithespeed.hellojava06.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.lithespeed.hellojava06.config.S3ServiceVirtualizationConfig;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;

/**
 * Integration test using WireMock to virtualize S3 service behavior.
 * This demonstrates service virtualization - testing our S3Controller
 * with AwsS3Service against mocked S3 responses without needing real AWS
 * infrastructure.
 * 
 * This test uses the 'service-virtualization' profile which configures
 * AwsS3Service to use a mocked S3Client pointing to WireMock instead of real
 * AWS.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "aws-s3", "service-virtualization" }) // Use real AwsS3Service but with mocked S3Client
@ContextConfiguration(classes = { S3ServiceVirtualizationConfig.class })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class S3ServiceVirtualizationTest {

    @LocalServerPort
    private int port;

    private static WireMockServer wireMockServer;

    @BeforeAll
    static void setupWireMock() {
        // Start WireMock server to mock S3 API
        wireMockServer = new WireMockServer(WireMockConfiguration.options()
                .port(9999)
                .usingFilesUnderClasspath("wiremock"));
        wireMockServer.start();
        WireMock.configureFor("localhost", 9999);

        System.out.println("WireMock S3 virtualization server started on port 9999");
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
    @DisplayName("S3 Health Check should return OK with service virtualization")
    void testS3HealthCheck() {
        // Test our actual endpoint (no S3 calls for health check)
        given()
                .port(port)
                .when()
                .get("/s3/health")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(2)
    @DisplayName("List files should work with empty S3 bucket simulation")
    void testListFilesWithEmptyBucket() {
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

        // Test our endpoint - should hit the mocked S3 API
        given()
                .port(port)
                .when()
                .get("/s3/list")
                .then()
                .statusCode(200);

        // Verify WireMock received the request
        verify(getRequestedFor(urlMatching("/test-bucket.*")));
    }

    @Test
    @Order(3)
    @DisplayName("Upload file should simulate successful S3 upload")
    void testFileUploadWithS3Mock() {
        // Mock S3 PutObject API response
        stubFor(put(urlMatching("/test-bucket/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("ETag", "\"d41d8cd98f00b204e9800998ecf8427e\"")
                        .withHeader("x-amz-request-id", "mock-request-id")
                        .withHeader("x-amz-id-2", "mock-extended-request-id")));

        // Test file upload endpoint
        given()
                .port(port)
                .queryParam("fileName", "test-document.pdf")
                .when()
                .post("/s3/upload")
                .then()
                .statusCode(200);

        // Verify WireMock received the upload request
        verify(putRequestedFor(urlMatching("/test-bucket/test-document.pdf.*")));
    }

    @Test
    @Order(4)
    @DisplayName("List files should show uploaded files from S3 mock")
    void testListFilesWithMockedContent() {
        // Mock S3 ListObjects API response with files
        stubFor(get(urlMatching("/test-bucket.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBody("""
                                <?xml version="1.0" encoding="UTF-8"?>
                                <ListObjectsV2Result xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
                                    <Name>test-bucket</Name>
                                    <Prefix></Prefix>
                                    <KeyCount>2</KeyCount>
                                    <MaxKeys>1000</MaxKeys>
                                    <IsTruncated>false</IsTruncated>
                                    <Contents>
                                        <Key>test-document.pdf</Key>
                                        <LastModified>2024-01-15T10:30:00.000Z</LastModified>
                                        <Size>1024</Size>
                                        <StorageClass>STANDARD</StorageClass>
                                        <ETag>"d41d8cd98f00b204e9800998ecf8427e"</ETag>
                                    </Contents>
                                    <Contents>
                                        <Key>another-file.txt</Key>
                                        <LastModified>2024-01-15T11:15:00.000Z</LastModified>
                                        <Size>512</Size>
                                        <StorageClass>STANDARD</StorageClass>
                                        <ETag>"098f6bcd4621d373cade4e832627b4f6"</ETag>
                                    </Contents>
                                </ListObjectsV2Result>
                                """)));

        // Test listing files
        given()
                .port(port)
                .when()
                .get("/s3/list")
                .then()
                .statusCode(200);

        // Verify the request was made to our mock
        verify(getRequestedFor(urlMatching("/test-bucket.*")));
    }

    @Test
    @Order(5)
    @DisplayName("Should handle S3 service errors gracefully")
    void testS3ServiceErrorHandling() {
        // Mock S3 service returning an error (throttling)
        stubFor(get(urlMatching("/test-bucket.*"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/xml")
                        .withBody("""
                                <?xml version="1.0" encoding="UTF-8"?>
                                <Error>
                                    <Code>SlowDown</Code>
                                    <Message>Please reduce your request rate.</Message>
                                    <RequestId>mock-request-id-503</RequestId>
                                    <HostId>mock-host-id</HostId>
                                </Error>
                                """)));

        // Our service should handle this error gracefully
        // This will actually test the AwsS3Service error handling
        given()
                .port(port)
                .when()
                .get("/s3/list")
                .then()
                .statusCode(500); // Our controller should return 500 for S3 errors

        // Verify the request was attempted
        verify(getRequestedFor(urlMatching("/test-bucket.*")));
    }

    @Test
    @Order(6)
    @DisplayName("Should handle S3 access denied errors")
    void testS3AccessDeniedError() {
        // Mock S3 access denied response
        stubFor(put(urlMatching("/test-bucket/.*"))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withHeader("Content-Type", "application/xml")
                        .withBody("""
                                <?xml version="1.0" encoding="UTF-8"?>
                                <Error>
                                    <Code>AccessDenied</Code>
                                    <Message>Access Denied</Message>
                                    <RequestId>mock-request-id-403</RequestId>
                                    <HostId>mock-host-id</HostId>
                                </Error>
                                """)));

        // Test upload with access denied
        given()
                .port(port)
                .queryParam("fileName", "denied-file.pdf")
                .when()
                .post("/s3/upload")
                .then()
                .statusCode(500); // Our controller should return 500 for S3 errors

        // Verify the request was attempted
        verify(putRequestedFor(urlMatching("/test-bucket/denied-file.pdf.*")));
    }
}
