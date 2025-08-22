Feature: Helper feature for uploading S3 files

Background:
  * url 'http://localhost:' + karate.properties['karate.server.port']

Scenario: Upload a file with given fileName
  Given path '/s3/upload'
  And param fileName = fileName  
  When method post
  Then status 200
  And match response == 'Uploaded: ' + fileName
