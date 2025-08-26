Feature: Health Check API Test

Background:
  * url 'http://localhost:' + karate.properties['karate.server.port']

Scenario: Verify health check endpoint is accessible
  Given path 'actuator/health'
  When method GET
  Then status 200
  And match response.status == 'UP'

Scenario: Verify application is running
  Given path 'actuator/info'
  When method GET
  Then status 200
