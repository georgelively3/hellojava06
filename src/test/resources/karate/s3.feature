Feature: S3 Controller Health Check

Background:
  * url 'http://localhost:' + karate.properties['karate.server.port']

Scenario: Verify S3 controller health endpoint is accessible
  Given path 's3/health'
  When method GET
  Then status 200
  And match response.status == 'UP'

Scenario: List files from S3 Bucket
  Given path 's3/list'
  When method GET
  Then status 200
  And match response.success == true
  And match response.count == '#number'
  And match response.files == '#[]'
  And match each response.files == '#string'

Scenario: Delete file from S3 Bucket
  Given path 's3/delete'
  And param key = 'test-file.txt'
  When method DELETE
  Then status 200
  And match response contains 'File deleted successfully'

Scenario: Delete file from S3 Bucket - File not found
  Given path 's3/delete'
  And param key = 'nonexistent-file.txt'
  When method DELETE
  Then status 200
  And match response contains 'File deleted successfully'

Scenario: Upload and Delete file lifecycle test
  # Upload a test file
  Given path 's3/upload-file'
  And multipart file file = { read: 'classpath:test-data/sample.txt', filename: 'test-upload.txt', contentType: 'text/plain' }
  When method POST
  Then status 200
  And match response.success == true
  And match response.message == 'File uploaded successfully'
  And match response.etag != null
  
  # Extract the uploaded file key from the etag
  * def uploadedKey = response.etag
  
  # Verify file exists
  Given path 's3/exists'
  And param key = uploadedKey
  When method GET
  Then status 200
  And match response == true
  
  # Clean up - delete the test file
  Given path 's3/delete'
  And param key = uploadedKey
  When method DELETE
  Then status 200
  And match response contains 'File deleted successfully'
  
  # Verify file no longer exists
  Given path 's3/exists'
  And param key = uploadedKey
  When method GET
  Then status 200
  And match response == false
