Feature: S3 API Testing

Background:
  * def baseUrl = 'http://localhost:' + karate.properties['karate.server.port']
  * url baseUrl
  * header Accept = 'application/json'

@integration @karate
Scenario: Upload file via S3 API
  Given path '/s3/upload'
  And param fileName = 'test-file.txt'
  When method post
  Then status 200
  And match response == 'Uploaded: test-file.txt'

@integration @karate
Scenario: List files via S3 API
  # First upload a file
  Given path '/s3/upload'
  And param fileName = 'list-test-file.txt'
  When method post
  Then status 200
  
  # Now list files
  Given path '/s3/list'
  When method get
  Then status 200
  And match response == '#array'
  And match response contains 'list-test-file.txt'

@integration @karate
Scenario: S3 Health Check
  Given path '/s3/health'
  When method get
  Then status 200
  And match response.status == 'UP'
