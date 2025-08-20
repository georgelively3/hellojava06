Feature: S3 API Testing

Background:
  * url baseUrl
  * header Accept = 'application/json'
  * header Content-Type = 'application/json'

Scenario: Upload file to S3
  Given path '/s3/upload'
  And param bucketName = 'test-bucket'
  And param key = 'test-file.txt'
  And param content = 'Hello, WireMock S3!'
  When method post
  Then status 200
  And match response == 'File uploaded successfully'

Scenario: Download file from S3
  # First upload a file
  Given path '/s3/upload'
  And param bucketName = 'test-bucket'
  And param key = 'download-test.txt'
  And param content = 'Download test content'
  When method post
  Then status 200
  
  # Now download the file
  Given path '/s3/download'
  And param bucketName = 'test-bucket'
  And param key = 'download-test.txt'
  When method get
  Then status 200
  And match response == 'Download test content'

Scenario: List S3 objects
  # First upload a couple of files
  Given path '/s3/upload'
  And param bucketName = 'test-bucket'
  And param key = 'list-test-1.txt'
  And param content = 'File 1 content'
  When method post
  Then status 200
  
  Given path '/s3/upload'
  And param bucketName = 'test-bucket'
  And param key = 'list-test-2.txt'
  And param content = 'File 2 content'
  When method post
  Then status 200
  
  # Now list objects
  Given path '/s3/list'
  And param bucketName = 'test-bucket'
  When method get
  Then status 200
  And match response == '#array'
  And match response[*] contains 'list-test-1.txt'
  And match response[*] contains 'list-test-2.txt'

Scenario: Delete file from S3
  # First upload a file
  Given path '/s3/upload'
  And param bucketName = 'test-bucket'
  And param key = 'delete-test.txt'
  And param content = 'Delete test content'
  When method post
  Then status 200
  
  # Delete the file
  Given path '/s3/delete'
  And param bucketName = 'test-bucket'
  And param key = 'delete-test.txt'
  When method delete
  Then status 200
  And match response == 'File deleted successfully'
  
  # Verify file is deleted - should return 404
  Given path '/s3/download'
  And param bucketName = 'test-bucket'
  And param key = 'delete-test.txt'
  When method get
  Then status 404
