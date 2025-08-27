Feature: Control-M Job Management API

  Background:
    * url baseUrl
    * def controlMBasePath = '/control-m'

  Scenario: Health check endpoint should be available
    Given path controlMBasePath + '/health'
    When method GET
    Then status 200
    And match response == { status: 'UP', service: 'Control-M Integration', timestamp: '#string' }

  Scenario: Start a simple job successfully
    Given path controlMBasePath + '/jobs/start'
    And param jobName = 'TestJob'
    When method POST
    Then status 200
    And match response.executionId == '#string'
    And match response.jobName == 'TestJob'
    And match response.status == 'STARTED'
    And match response.timestamp == '#string'
    And match response.message == '#string'
    * def executionId = response.executionId

  Scenario: Start job with custom ID
    Given path controlMBasePath + '/jobs/start'
    And param jobName = 'CustomIdJob'
    And param jobId = 'custom-execution-123'
    When method POST
    Then status 200
    And match response.executionId == 'custom-execution-123'
    And match response.jobName == 'CustomIdJob'
    And match response.status == 'STARTED'

  Scenario: Start job with parameters
    Given path controlMBasePath + '/jobs/start'
    And param jobName = 'ParameterizedJob'
    And request { reportDate: '2025-01-15', format: 'PDF', priority: 'HIGH' }
    When method POST
    Then status 200
    And match response.jobName == 'ParameterizedJob'
    And match response.status == 'STARTED'
    * def executionId = response.executionId

  Scenario: Get job status for valid execution ID
    # First start a job
    Given path controlMBasePath + '/jobs/start'
    And param jobName = 'StatusCheckJob'
    When method POST
    Then status 200
    * def executionId = response.executionId
    
    # Then check its status
    Given path controlMBasePath + '/jobs/' + executionId + '/status'
    When method GET
    Then status 200
    And match response.executionId == executionId
    And match response.jobName == 'StatusCheckJob'
    And match response.status == '#string'
    And match response.startTime == '#string'
    And match response.duration == '#string'
    And match response.parameters == '#object'

  Scenario: Get job status for invalid execution ID should return 404
    Given path controlMBasePath + '/jobs/invalid-execution-id/status'
    When method GET
    Then status 404

  Scenario: Complete a job manually
    # First start a job
    Given path controlMBasePath + '/jobs/start'
    And param jobName = 'ManualCompleteJob'
    When method POST
    Then status 200
    * def executionId = response.executionId
    
    # Then complete it manually
    Given path controlMBasePath + '/jobs/' + executionId + '/complete'
    And param status = 'SUCCESS'
    And request { recordsProcessed: 100, outputFile: 'report.pdf' }
    When method POST
    Then status 200
    And match response.executionId == executionId
    And match response.jobName == 'ManualCompleteJob'
    And match response.status == 'SUCCESS'
    And match response.completedAt == '#string'
    And match response.duration == '#string'
    And match response.results.recordsProcessed == 100

  Scenario: List all jobs
    # Start a few jobs first
    Given path controlMBasePath + '/jobs/start'
    And param jobName = 'ListTestJob1'
    When method POST
    Then status 200
    
    Given path controlMBasePath + '/jobs/start'
    And param jobName = 'ListTestJob2'
    When method POST
    Then status 200
    
    # Then list all jobs
    Given path controlMBasePath + '/jobs'
    When method GET
    Then status 200
    And match response.jobs == '#[]'
    And match response.total == '#number'
    And match response.filter == 'ALL'

  Scenario: List jobs with status filter
    Given path controlMBasePath + '/jobs'
    And param status = 'RUNNING'
    When method GET
    Then status 200
    And match response.filter == 'RUNNING'

  Scenario: Get job logs
    # First start a job
    Given path controlMBasePath + '/jobs/start'
    And param jobName = 'LogTestJob'
    When method POST
    Then status 200
    * def executionId = response.executionId
    
    # Then get its logs
    Given path controlMBasePath + '/jobs/' + executionId + '/logs'
    When method GET
    Then status 200
    And match response.executionId == executionId
    And match response.logs == '#[]'
    And match response.logCount == '#number'

  Scenario: Start batch jobs
    Given path controlMBasePath + '/batch/start'
    And request 
    """
    [
      {
        "jobName": "BatchJob1",
        "parameters": {
          "source": "database1"
        }
      },
      {
        "jobName": "BatchJob2",
        "parameters": {
          "format": "json"
        }
      },
      {
        "jobName": "BatchJob3",
        "parameters": {
          "target": "warehouse"
        }
      }
    ]
    """
    When method POST
    Then status 200
    And match response.batchId == '#string'
    And match response.jobsStarted == 3
    And match response.jobs == '#[3]'
    And match response.timestamp == '#string'

  Scenario: Start empty batch should succeed
    Given path controlMBasePath + '/batch/start'
    And request []
    When method POST
    Then status 200
    And match response.jobsStarted == 0
    And match response.jobs == '#[0]'

  Scenario: Cancel a running job
    # First start a job
    Given path controlMBasePath + '/jobs/start'
    And param jobName = 'CancelTestJob'
    When method POST
    Then status 200
    * def executionId = response.executionId
    
    # Then cancel it
    Given path controlMBasePath + '/jobs/' + executionId
    When method DELETE
    Then status 200
    And match response.executionId == executionId
    And match response.jobName == 'CancelTestJob'
    And match response.status == 'CANCELLED'
    And match response.message == '#string'

  Scenario: Cancel invalid job should return 404
    Given path controlMBasePath + '/jobs/invalid-execution-id'
    When method DELETE
    Then status 404

  Scenario: Job simulation workflow - Quick job should complete automatically
    # Start a quick test job that should complete automatically
    Given path controlMBasePath + '/jobs/start'
    And param jobName = 'QuickTest'
    When method POST
    Then status 200
    * def executionId = response.executionId
    
    # Wait for job to complete (simulation)
    * def sleep = function(millis){ java.lang.Thread.sleep(millis) }
    * call sleep 3000
    
    # Check that job completed
    Given path controlMBasePath + '/jobs/' + executionId + '/status'
    When method GET
    Then status 200
    And match response.status == '#regex (SUCCESS|FAILED)'
    And match response.endTime == '#string'

  Scenario: Full workflow test - Data processing pipeline
    # Start a data extract job
    Given path controlMBasePath + '/jobs/start'
    And param jobName = 'DataExtract'
    And request { source: 'production-db', table: 'orders' }
    When method POST
    Then status 200
    * def extractJobId = response.executionId
    
    # Start a data transform job
    Given path controlMBasePath + '/jobs/start'
    And param jobName = 'DataTransform'
    And request { inputFile: 'orders.csv', format: 'parquet' }
    When method POST
    Then status 200
    * def transformJobId = response.executionId
    
    # Start a data load job
    Given path controlMBasePath + '/jobs/start'
    And param jobName = 'DataLoad'
    And request { target: 'data-warehouse', schema: 'analytics' }
    When method POST
    Then status 200
    * def loadJobId = response.executionId
    
    # Wait for jobs to process
    * call sleep 6000
    
    # Verify all jobs have completed or are processing
    Given path controlMBasePath + '/jobs/' + extractJobId + '/status'
    When method GET
    Then status 200
    And match response.status == '#regex (RUNNING|SUCCESS|FAILED)'
    
    Given path controlMBasePath + '/jobs/' + transformJobId + '/status'
    When method GET
    Then status 200
    And match response.status == '#regex (RUNNING|SUCCESS|FAILED)'
    
    Given path controlMBasePath + '/jobs/' + loadJobId + '/status'
    When method GET
    Then status 200
    And match response.status == '#regex (RUNNING|SUCCESS|FAILED)'
    
    # Check that all jobs have logs
    Given path controlMBasePath + '/jobs/' + extractJobId + '/logs'
    When method GET
    Then status 200
    And match response.logs == '#[]'
    And response.logCount > 0
