package com.lithespeed.hellojava06.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import jakarta.annotation.PreDestroy;
import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Test configuration for WireMock-based S3 service virtualization
 * This replaces LocalStack with lightweight HTTP mocks
 */
@Configuration
@Profile("mountebank")
public class MountebankS3Config {

    @Value("${wiremock.port:8089}")
    private int wireMockPort;

    @Value("${aws.s3.endpoint:http://localhost:8089}")
    private String s3Endpoint;

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    @Value("${aws.s3.bucket-name:test-bucket}")
    private String bucketName;

    private WireMockServer wireMockServer;

    @Bean
    public WireMockServer wireMockServer() {
        if (wireMockServer == null) {
            wireMockServer = new WireMockServer(WireMockConfiguration.options()
                    .port(wireMockPort)
                    .disableRequestJournal());
            wireMockServer.start();
            setupS3MockEndpoints();
        }
        return wireMockServer;
    }

    @Bean
    public S3Client s3Client() {
        // Ensure WireMock server is started
        wireMockServer();

        return S3Client.builder()
                .endpointOverride(URI.create(s3Endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("mock-access-key", "mock-secret-key")))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    private void setupS3MockEndpoints() {
        WireMock.configureFor("localhost", wireMockPort);

        // Health check endpoint - List buckets
        stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<ListAllMyBucketsResult>" +
                                "<Owner><ID>mock</ID><DisplayName>mock</DisplayName></Owner>" +
                                "<Buckets><Bucket><Name>" + bucketName + "</Name>" +
                                "<CreationDate>2024-01-01T00:00:00Z</CreationDate></Bucket></Buckets>" +
                                "</ListAllMyBucketsResult>")));

        // List bucket contents
        stubFor(get(urlEqualTo("/" + bucketName))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<ListBucketResult>" +
                                "<Name>" + bucketName + "</Name>" +
                                "<Contents>" +
                                "<Key>test-file.txt</Key>" +
                                "<Size>13</Size>" +
                                "<LastModified>2024-01-01T00:00:00Z</LastModified>" +
                                "</Contents>" +
                                "</ListBucketResult>")));

        // Upload file (PUT) - matches any file in the bucket
        stubFor(put(urlMatching("/" + bucketName + "/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("ETag", "\"d41d8cd98f00b204e9800998ecf8427e\"")));

        // Get file - matches any file in the bucket
        stubFor(get(urlMatching("/" + bucketName + "/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/octet-stream")
                        .withBody("Hello, World!")));

        // Delete file - matches any file in the bucket
        stubFor(delete(urlMatching("/" + bucketName + "/.*"))
                .willReturn(aResponse()
                        .withStatus(204)));

        // Handle bucket existence check (HEAD request)
        stubFor(head(urlEqualTo("/" + bucketName))
                .willReturn(aResponse()
                        .withStatus(200)));
    }

    @PreDestroy
    public void cleanup() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getS3Endpoint() {
        return s3Endpoint;
    }

    public int getWireMockPort() {
        return wireMockPort;
    }
}
