Feature: Control-M API Integration with Mountebank

Background:
  * url 'http://localhost:8080'

Scenario: Health Check - Control-M Integration Service
  Given path '/control-m/health'
  When method GET
  Then status 200
  And match response.status == 'UP'
  And match response.service == 'Control-M Integration'
  And match response.timestamp == '#present'
  And match response.controlMApiUrl == 'http://localhost:2525'

Scenario: Start Job - ETL Job Type
  Given path '/control-m/jobs/start'
  And param jobName = 'ETL_DAILY_LOAD'
  When method POST
  Then status 200
  And match response.executionId == '#uuid'
  And match response.jobName == 'ETL_DAILY_LOAD'
  And match response.status == 'STARTED'
  And match response.controlMJobId == '#regex CTM_ETL_.*'
  And match response.timestamp == '#present'

Scenario: Start Job - Report Job Type
  Given path '/control-m/jobs/start'
  And param jobName = 'REPORT_GENERATION'
  When method POST
  Then status 200
  And match response.executionId == '#uuid'
  And match response.jobName == 'REPORT_GENERATION'
  And match response.status == 'STARTED'
  And match response.controlMJobId == '#regex CTM_REPORT_.*'
  And match response.estimatedDuration == '10 minutes'

Scenario: Start Job - Backup Job Type
  Given path '/control-m/jobs/start'
  And param jobName = 'BACKUP_NIGHTLY'
  When method POST
  Then status 200
  And match response.executionId == '#uuid'
  And match response.jobName == 'BACKUP_NIGHTLY'
  And match response.status == 'STARTED'
  And match response.controlMJobId == '#regex CTM_BACKUP_.*'
  And match response.estimatedDuration == '30 minutes'

Scenario: List Jobs - All Jobs
  Given path '/control-m/jobs'
  When method GET
  Then status 200
  And match response.jobs == '#array'
  And match response.total == '#number'
  And match response.filter == 'ALL'

Scenario: Get Job Status - Invalid Execution ID
  Given path '/control-m/jobs/invalid-job-id/status'
  When method GET
  Then status 404

Scenario: Validate Mountebank Stub Responses
  # Test that our stubs are working correctly by calling the Control-M API directly
  Given url 'http://localhost:2525'
  And path '/control-m/jobs/submit'
  And request { jobName: 'ETL_TEST', parameters: {} }
  When method POST
  Then status 200
  And match response.controlMJobId == '#regex CTM_ETL_.*'
  And match response.status == 'SUBMITTED'
  And match response.estimatedDuration == '15 minutes'
