Feature: External API Integration Testing with WireMock

Background:
  * def baseUrl = 'http://localhost:' + karate.properties['karate.server.port']
  * def wireMockUrl = 'http://localhost:' + karate.properties['wiremock.port']
  * header Accept = 'application/json'

@integration @wiremock
Scenario: Test external health check via WireMock
  Given url wireMockUrl
  And path '/external/health'
  When method get
  Then status 200
  And match response == { status: 'UP', service: 'external-mock', timestamp: '#present' }

@integration @wiremock
Scenario: Test external notification service via WireMock
  Given url wireMockUrl
  And path '/external/api/notify'
  And request { message: 'Test notification', recipient: 'test@example.com' }
  When method post
  Then status 201
  And match response == { notified: true, messageId: 'mock-12345' }

@integration @wiremock
Scenario: Test external file processing via WireMock
  Given url wireMockUrl
  And path '/external/process/test-file.txt'
  And request { action: 'process', priority: 'high' }
  When method post
  Then status 200
  And match response == { processed: true, status: 'completed' }

@integration @karate
Scenario: Test application health endpoint (app-level test)
  Given url baseUrl
  And path '/api/health'
  When method get
  Then status 200
  And match response.status == 'UP'
  And match response.service == 'hellojava06'

@integration @karate
Scenario: Integration test combining app and external services
  # First test our app's S3 functionality
  Given url baseUrl
  And path '/s3/upload'
  And param fileName = 'integration-test-file.txt'
  When method post
  Then status 200
  And match response == 'Uploaded: integration-test-file.txt'
  
  # Then test external notification (via WireMock)
  Given url wireMockUrl
  And path '/external/api/notify'
  And request { message: 'File uploaded successfully', file: 'integration-test-file.txt' }
  When method post
  Then status 201
  And match response.notified == true
