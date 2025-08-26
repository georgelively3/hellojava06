Feature: S3 Controller Health Check

Background:
  * url 'http://localhost:' + karate.properties['karate.server.port']

Scenario: Verify S3 controller health endpoint is accessible
  Given path 's3/health'
  When method GET
  Then status 200
  And match response.status == 'UP'
