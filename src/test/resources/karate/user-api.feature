Feature: User API Tests - Karate Implementation

  Background:
    * url baseUrl
    
  Scenario: Health check - Get all users when none exist
    Given path '/api/users'
    When method GET
    Then status 200
    And match response == []
    
  Scenario: Create a new user
    Given path '/api/users'
    And request 
      """
      {
        "username": "johndoe",
        "email": "john@example.com",
        "firstName": "John",
        "lastName": "Doe"
      }
      """
    When method POST
    Then status 201
    And match response.username == 'johndoe'
    And match response.email == 'john@example.com'
    And match response.firstName == 'John'
    And match response.lastName == 'Doe'
    And match response.id == '#number'
    * def userId = response.id
    
    # Verify user was saved by getting it
    Given path '/api/users', userId
    When method GET
    Then status 200
    And match response.username == 'johndoe'
    
  Scenario: Create user with invalid data should fail
    Given path '/api/users'
    And request 
      """
      {
        "username": "",
        "email": "invalid-email"
      }
      """
    When method POST
    Then status 400
    And match response.message == '#string'
    
  Scenario: Get user by username
    # First create a user
    Given path '/api/users'
    And request 
      """
      {
        "username": "testuser",
        "email": "test@example.com",
        "firstName": "Test",
        "lastName": "User"
      }
      """
    When method POST
    Then status 201
    * def userId = response.id
    
    # Then get by username
    Given path '/api/users/username/testuser'
    When method GET
    Then status 200
    And match response.username == 'testuser'
    And match response.email == 'test@example.com'
    
  Scenario: Update user
    # First create a user
    Given path '/api/users'
    And request 
      """
      {
        "username": "updateuser",
        "email": "update@example.com",
        "firstName": "Update",
        "lastName": "User"
      }
      """
    When method POST
    Then status 201
    * def userId = response.id
    
    # Update the user
    Given path '/api/users', userId
    And request 
      """
      {
        "username": "updateuser",
        "email": "updated@example.com",
        "firstName": "Updated",
        "lastName": "User"
      }
      """
    When method PUT
    Then status 200
    And match response.email == 'updated@example.com'
    And match response.firstName == 'Updated'
    
  Scenario: Delete user
    # First create a user
    Given path '/api/users'
    And request 
      """
      {
        "username": "deleteuser",
        "email": "delete@example.com",
        "firstName": "Delete",
        "lastName": "User"
      }
      """
    When method POST
    Then status 201
    * def userId = response.id
    
    # Delete the user
    Given path '/api/users', userId
    When method DELETE
    Then status 200
    And match response.message == 'User deleted successfully'
    
    # Verify user is deleted
    Given path '/api/users', userId
    When method GET
    Then status 404
    
  Scenario: Search users by name
    # Create test users
    Given path '/api/users'
    And request 
      """
      {
        "username": "john1",
        "email": "john1@example.com",
        "firstName": "John",
        "lastName": "Smith"
      }
      """
    When method POST
    Then status 201
    
    Given path '/api/users'
    And request 
      """
      {
        "username": "john2",
        "email": "john2@example.com",
        "firstName": "Johnny",
        "lastName": "Doe"
      }
      """
    When method POST
    Then status 201
    
    # Search for users named John
    Given path '/api/users/search'
    And param name = 'John'
    When method GET
    Then status 200
    And match response == '#[2]'
    And match response[*].firstName contains 'John'
