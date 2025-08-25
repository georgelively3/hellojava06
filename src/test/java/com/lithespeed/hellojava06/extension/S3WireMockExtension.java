package com.lithespeed.hellojava06.extension;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * JUnit 5 extension for managing WireMock server lifecycle
 * and setting up common S3 API mocks.
 */
public class S3WireMockExtension implements BeforeAllCallback, AfterAllCallback {

    private static WireMockServer wireMockServer;
    private static final int WIREMOCK_PORT = 9090;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (wireMockServer == null || !wireMockServer.isRunning()) {
            startWireMockServer();
        }
        setupS3Mocks();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        // We'll keep the server running between test classes
        // Only stop when JVM shuts down
        if (wireMockServer != null) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (wireMockServer.isRunning()) {
                    wireMockServer.stop();
                    System.out.println("WireMock server stopped on shutdown");
                }
            }));
        }
    }

    private void startWireMockServer() {
        try {
            wireMockServer = new WireMockServer(WireMockConfiguration.options()
                    .port(WIREMOCK_PORT)
                    .usingFilesUnderClasspath("wiremock"));

            wireMockServer.start();
            WireMock.configureFor("localhost", WIREMOCK_PORT);

            System.out.println("WireMock S3 server started on port " + WIREMOCK_PORT);
            System.out.println("WireMock server is running: " + wireMockServer.isRunning());
            System.out.println("WireMock base URL: " + wireMockServer.baseUrl());
        } catch (Exception e) {
            System.err.println("Failed to start WireMock server: " + e.getMessage());
            throw new RuntimeException("Could not start WireMock server", e);
        }
    }

    private void setupS3Mocks() {
        try {
            // Reset all previous stubs
            wireMockServer.resetAll();

            // Mock S3 ListObjects API response for bucket with test files  
            // Use a very broad pattern to catch all requests and log them
            wireMockServer.stubFor(get(urlMatching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/xml")
                            .withHeader("x-amz-request-id", "mock-request-id")
                            .withHeader("x-amz-id-2", "mock-extended-request-id")
                            .withBody("""
                                    <?xml version="1.0" encoding="UTF-8"?>
                                    <ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
                                        <Name>test-bucket</Name>
                                        <Prefix></Prefix>
                                        <Marker></Marker>
                                        <MaxKeys>1000</MaxKeys>
                                        <IsTruncated>false</IsTruncated>
                                        <Contents>
                                            <Key>list-test-file.txt</Key>
                                            <LastModified>2023-01-01T00:00:00.000Z</LastModified>
                                            <ETag>"d41d8cd98f00b204e9800998ecf8427e"</ETag>
                                            <Size>13</Size>
                                            <StorageClass>STANDARD</StorageClass>
                                        </Contents>
                                        <Contents>
                                            <Key>test-file.txt</Key>
                                            <LastModified>2023-01-01T00:00:00.000Z</LastModified>
                                            <ETag>"85cc3110c5b74da17abb46e7dbe6b5f5"</ETag>
                                            <Size>13</Size>
                                            <StorageClass>STANDARD</StorageClass>
                                        </Contents>
                                    </ListBucketResult>
                                    """)));

            // Mock S3 PutObject API response with correct MD5 hash for "dummy content"
            // MD5 hash of "dummy content" is: 85cc3110c5b74da17abb46e7dbe6b5f5  
            wireMockServer.stubFor(put(urlMatching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/xml")
                            .withHeader("ETag", "\"85cc3110c5b74da17abb46e7dbe6b5f5\"")
                            .withHeader("x-amz-request-id", "mock-request-id")
                            .withHeader("x-amz-id-2", "mock-extended-request-id")
                            .withBody(""))); // Empty body for PutObject response

            // Mock S3 HeadBucket for bucket existence checks
            wireMockServer.stubFor(head(urlEqualTo("/test-bucket"))
                    .willReturn(aResponse()
                            .withStatus(200)));

            System.out.println("S3 WireMock stubs configured successfully");
        } catch (Exception e) {
            System.err.println("Failed to setup S3 mocks: " + e.getMessage());
            throw new RuntimeException("Could not setup S3 mocks", e);
        }
    }

    public static boolean isWireMockRunning() {
        return wireMockServer != null && wireMockServer.isRunning();
    }

    public static int getWireMockPort() {
        return WIREMOCK_PORT;
    }
}
