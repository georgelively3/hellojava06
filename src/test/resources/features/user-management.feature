Feature: User Management
  As a system administrator
  I want to manage users in the system
  So that I can maintain user data effectively

  Background:
    Given the system has no users

  Scenario: Create a new user successfully
    Given I have a user with username "johndoe", email "john@example.com", first name "John", and last name "Doe"
    When I create the user
    Then the response status should be 201
    And the user should be saved in the database

  Scenario: Retrieve all users when no users exist
    When I request all users
    Then the response status should be 200
    And the response should contain 0 users

  Scenario: Retrieve all users when users exist
    Given the system has a user with username "user1" and email "user1@example.com"
    And the system has a user with username "user2" and email "user2@example.com"
    When I request all users
    Then the response status should be 200
    And the response should contain 2 users

  Scenario: Retrieve a specific user by username
    Given the system has a user with username "testuser" and email "test@example.com"
    When I request user with username "testuser"
    Then the response status should be 200
    And the response should contain user with username "testuser"
    And the response should contain user with email "test@example.com"

  Scenario: Search for users by name
    Given the system has a user with username "johndoe" and email "john@example.com"
    When I search for users with name "Test"
    Then the response status should be 200
    And the response should contain 1 users

  Scenario: Update user information
    Given the system has a user with username "updateuser" and email "old@example.com"
    When I update user with ID 1 to have email "new@example.com"
    Then the response status should be 200
    And the response should contain user with email "new@example.com"

  Scenario: Delete a user
    Given the system has a user with username "deleteuser" and email "delete@example.com"
    When I delete user with ID 1
    Then the response status should be 200
    And the response should contain success message "User deleted successfully"
    And the user should be removed from the database

  Scenario: Attempt to create user with duplicate username
    Given the system has a user with username "duplicate" and email "existing@example.com"
    And I have a user with username "duplicate", email "new@example.com", first name "New", and last name "User"
    When I create the user
    Then the response status should be 409

  Scenario: Attempt to retrieve non-existent user
    When I request user with ID 999
    Then the response status should be 404
