Feature: S3 API Testing

Background:
  * url baseUrl
  * header Accept = 'application/json'

Scenario: S3 Health Check
  Given path '/api/s3/health'
  When method get
  Then status 200
  And match response contains { status: 'UP', service: 'S3' }

Scenario: List S3 files handles connection failure gracefully
  Given path '/api/s3/list'
  When method get
  Then status 500
  And match response contains { error: '#string' }
