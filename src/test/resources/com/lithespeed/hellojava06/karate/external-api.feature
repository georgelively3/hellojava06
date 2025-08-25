Feature: External API Integration Testing (Environment Agnostic)

Background:
  * def baseUrl = 'http://localhost:' + karate.properties['karate.server.port']
  * def externalServiceUrl = karate.properties['external.service.url'] || 'http://localhost:9999'
  * header Accept = 'application/json'

@integration @karate
Scenario: Test external health check (INT: LocalStack, PREPROD: Real Service)
  Given url externalServiceUrl
  And path '/external/health'
  When method get
  Then status 200
  And match response.status == 'UP'
  # Note: response.environment will be "mocked" in INT, "production" in PREPROD

@integration @karate
Scenario: Test external notification service (Environment Agnostic)
  Given url externalServiceUrl
  And path '/external/api/notify'
  And request { message: 'Test notification', recipient: 'test@example.com' }
  When method post
  Then status 201
  And match response.success == true

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
  
  # Then test external notification 
  # INT: hits LocalStack stub, PREPROD: hits real external service
  Given url externalServiceUrl
  And path '/external/api/notify'
  And request { message: 'File uploaded successfully', file: 'integration-test-file.txt' }
  When method post
  Then status 201
  And match response.success == true
