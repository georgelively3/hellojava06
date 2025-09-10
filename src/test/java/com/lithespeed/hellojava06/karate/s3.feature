Feature: S3 Controller Operations

Background:
  * url baseUrl
  * header Accept = 'application/json'

Scenario: Verify S3 controller health endpoint is accessible
  Given path 's3/health'
  When method GET
  Then status 200
  And match response.status == 'UP'

@preprod
Scenario: Upload a text file successfully
  Given path 's3/upload-file'
  And multipart file file = { read: 'classpath:test-files/sample.txt', filename: 'sample.txt', contentType: 'text/plain' }
  When method POST
  Then status 200
  And match response.success == true
  And match response.message == 'File uploaded successfully'
  And match response.fileName == 'sample.txt'
  And match response.contentType == 'text/plain'
  And match response.size == '#number'
  And match response.etag == '#string'
  And match response.etag contains 'uploads/'
  And match response.etag contains '.txt'

@preprod  
Scenario: Upload an image file successfully
  Given path 's3/upload-file'
  And multipart file file = { read: 'classpath:test-files/test-image.jpg', filename: 'test-image.jpg', contentType: 'image/jpeg' }
  When method POST
  Then status 200
  And match response.success == true
  And match response.message == 'File uploaded successfully'
  And match response.fileName == 'test-image.jpg'
  And match response.contentType == 'image/jpeg'
  And match response.size == '#number'
  And match response.etag == '#string'
  And match response.etag contains 'uploads/'
  And match response.etag contains '.jpg'

@preprod
Scenario: Upload a file without extension
  Given path 's3/upload-file'
  And multipart file file = { read: 'classpath:test-files/no-extension', filename: 'no-extension', contentType: 'application/octet-stream' }
  When method POST
  Then status 200
  And match response.success == true
  And match response.message == 'File uploaded successfully'
  And match response.fileName == 'no-extension'
  And match response.contentType == 'application/octet-stream'
  And match response.size == '#number'
  And match response.etag == '#string'
  # Should not have extension in the key
  And match response.etag contains 'uploads/'
  And match response.etag !contains '.jpg'
  And match response.etag !contains '.txt'

@preprod
Scenario: Upload empty file should still work
  Given path 's3/upload-file'
  And multipart file file = { read: 'classpath:test-files/empty.txt', filename: 'empty.txt', contentType: 'text/plain' }
  When method POST
  Then status 200
  And match response.success == true
  And match response.message == 'File uploaded successfully'
  And match response.fileName == 'empty.txt'
  And match response.size == 0

@preprod
Scenario: Fail to upload when no file provided
  Given path 's3/upload-file'
  When method POST
  Then status 400

@preprod
Scenario: List files from S3 bucket
  Given path 's3/list'
  When method GET
  Then status 200
  And match response == '#[]'
  And match each response == '#string'

@preprod
Scenario: Upload multiple files and verify listing
  # Upload first file
  Given path 's3/upload-file'
  And multipart file file = { read: 'classpath:test-files/sample.txt', filename: 'first-file.txt', contentType: 'text/plain' }
  When method POST
  Then status 200
  And def firstFileKey = response.etag
  
  # Upload second file
  Given path 's3/upload-file'
  And multipart file file = { read: 'classpath:test-files/test-image.jpg', filename: 'second-file.jpg', contentType: 'image/jpeg' }
  When method POST
  Then status 200
  And def secondFileKey = response.etag
  
  # List files and verify both are present
  Given path 's3/list'
  When method GET
  Then status 200
  And match response == '#[]'
  And match response contains firstFileKey
  And match response contains secondFileKey

@preprod
Scenario: Upload file with special characters in name
  Given path 's3/upload-file'
  And multipart file file = { read: 'classpath:test-files/sample.txt', filename: 'special-chars_file(1).txt', contentType: 'text/plain' }
  When method POST
  Then status 200
  And match response.success == true
  And match response.fileName == 'special-chars_file(1).txt'
  And match response.etag == '#string'

@preprod
Scenario: Upload large file (if test file exists)
  Given path 's3/upload-file'
  And multipart file file = { read: 'classpath:test-files/large-file.txt', filename: 'large-file.txt', contentType: 'text/plain' }
  When method POST
  Then status 200
  And match response.success == true
  And match response.message == 'File uploaded successfully'
  And match response.size == '#number'
  And assert response.size > 1000

@preprod
Scenario: Verify unique file keys for multiple uploads of same file
  # Upload same file twice
  Given path 's3/upload-file'
  And multipart file file = { read: 'classpath:test-files/sample.txt', filename: 'duplicate.txt', contentType: 'text/plain' }
  When method POST
  Then status 200
  And def firstKey = response.etag
  
  Given path 's3/upload-file'  
  And multipart file file = { read: 'classpath:test-files/sample.txt', filename: 'duplicate.txt', contentType: 'text/plain' }
  When method POST
  Then status 200
  And def secondKey = response.etag
  
  # Keys should be different (UUIDs make them unique)
  And assert firstKey != secondKey
  And match firstKey contains 'uploads/'
  And match secondKey contains 'uploads/'
