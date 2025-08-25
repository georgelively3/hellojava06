Feature: S3 API Testing with LocalStack

Background:
  * url 'http://localhost:' + karate.properties['karate.server.port']
  * header Accept = 'application/json'

Scenario: S3 Health Check
  Given path '/s3/health'
  When method get
  Then status 200
  And match response.status == 'UP'

Scenario: List files when empty
  Given path '/s3/list'
  When method get  
  Then status 200
  And match response == '#array'
  And match response == []

Scenario: Upload single file
  Given path '/s3/upload'
  And param fileName = 'test-file-1.txt'
  When method post
  Then status 200
  And match response == 'Uploaded: test-file-1.txt'

Scenario: Upload file and verify in list
  # Upload a file
  Given path '/s3/upload'
  And param fileName = 'karate-test.txt'
  When method post
  Then status 200
  And match response == 'Uploaded: karate-test.txt'
  
  # Check it appears in the list
  Given path '/s3/list'
  When method get
  Then status 200
  And match response == '#array'
  And match response contains 'karate-test.txt'
  And match response == '#[1]'

Scenario: Upload multiple files and verify list
  # Upload first file
  Given path '/s3/upload'
  And param fileName = 'file1.txt'
  When method post
  Then status 200
  
  # Upload second file
  Given path '/s3/upload' 
  And param fileName = 'file2.txt'
  When method post
  Then status 200
  
  # Upload third file
  Given path '/s3/upload'
  And param fileName = 'file3.txt' 
  When method post
  Then status 200
  
  # Verify all files in list
  Given path '/s3/list'
  When method get
  Then status 200
  And match response == '#array'
  And match response == '#[3]'
  And match response contains 'file1.txt'
  And match response contains 'file2.txt'
  And match response contains 'file3.txt'

Scenario: Upload file with missing fileName parameter
  Given path '/s3/upload'
  When method post
  Then status 400

Scenario: Test API with different file types
  # Test text file
  Given path '/s3/upload'
  And param fileName = 'document.txt'
  When method post
  Then status 200
  
  # Test image file
  Given path '/s3/upload'
  And param fileName = 'image.jpg'
  When method post
  Then status 200
  
  # Test JSON file
  Given path '/s3/upload'
  And param fileName = 'data.json'
  When method post
  Then status 200
  
  # Verify all different types are listed
  Given path '/s3/list'
  When method get
  Then status 200
  And match response contains 'document.txt'
  And match response contains 'image.jpg'
  And match response contains 'data.json'

Scenario: Test file names with special characters
  # Test file with spaces
  Given path '/s3/upload'
  And param fileName = 'my file with spaces.txt'
  When method post
  Then status 200
  
  # Test file with underscores
  Given path '/s3/upload'
  And param fileName = 'my_file_with_underscores.txt'
  When method post
  Then status 200
  
  # Test file with hyphens
  Given path '/s3/upload'
  And param fileName = 'my-file-with-hyphens.txt'
  When method post
  Then status 200
  
  # Verify special character files are listed
  Given path '/s3/list'
  When method get
  Then status 200
  And match response contains 'my file with spaces.txt'
  And match response contains 'my_file_with_underscores.txt'  
  And match response contains 'my-file-with-hyphens.txt'

Scenario: Basic performance test - upload multiple files
  Given path '/s3/upload'
  And param fileName = 'perf-test-1.txt'
  When method post
  Then status 200
  
  Given path '/s3/upload'
  And param fileName = 'perf-test-2.txt'
  When method post
  Then status 200
  
  Given path '/s3/upload'
  And param fileName = 'perf-test-3.txt'
  When method post
  Then status 200
  
  # Verify all files are listed
  Given path '/s3/list'
  When method get
  Then status 200
  And match response == '#array'
  And match response contains 'perf-test-1.txt'
  And match response contains 'perf-test-2.txt'
  And match response contains 'perf-test-3.txt'
