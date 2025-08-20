Feature: S3 API Tests - Karate Implementation with WireMock

  Background:
    * url baseUrl
    
  Scenario: S3 Health check
    Given path '/api/s3/health'
    When method GET
    Then status 200
    And match response.status == 'UP'
    And match response.bucketName == 'test-bucket'
    
  Scenario: Upload file to S3
    Given path '/api/s3/upload'
    And multipart file file = { read: 'classpath:test-data/sample-file.txt', filename: 'sample-file.txt', contentType: 'text/plain' }
    And multipart field fileName = 'uploaded-file.txt'
    When method POST
    Then status 200
    And match response.message == '#string'
    And match response.fileName == 'uploaded-file.txt'
    And match response.fileUrl == '#string'
    
  Scenario: List S3 files
    Given path '/api/s3/files'
    When method GET
    Then status 200
    And match response == '#[]'
    And match response[*].key == '#string'
    And match response[*].size == '#number'
    And match response[*].lastModified == '#string'
