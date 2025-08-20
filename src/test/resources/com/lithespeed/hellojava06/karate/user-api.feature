Feature: User API Testing

Background:
  * url baseUrl
  * header Accept = 'application/json'
  * header Content-Type = 'application/json'

Scenario: Create a new user
  Given path '/api/users'
  And request { username: 'johndoe', email: 'john.doe@example.com', firstName: 'John', lastName: 'Doe' }
  When method post
  Then status 201
  And match response == { id: '#present', username: 'johndoe', email: 'john.doe@example.com', firstName: 'John', lastName: 'Doe', createdAt: '#present', updatedAt: '#present' }

Scenario: Get all users
  Given path '/api/users'
  When method get
  Then status 200
  And match response == '#array'

Scenario: Get user by ID
  # First create a user
  Given path '/api/users'
  And request { username: 'janesmith', email: 'jane.smith@example.com', firstName: 'Jane', lastName: 'Smith' }
  When method post
  Then status 201
  And def userId = response.id
  
  # Now get the user by ID
  Given path '/api/users/' + userId
  When method get
  Then status 200
  And match response == { id: '#(userId)', username: 'janesmith', email: 'jane.smith@example.com', firstName: 'Jane', lastName: 'Smith', createdAt: '#present', updatedAt: '#present' }

Scenario: Update user
  # First create a user
  Given path '/api/users'
  And request { username: 'bobwilson', email: 'bob.wilson@example.com', firstName: 'Bob', lastName: 'Wilson' }
  When method post
  Then status 201
  And def userId = response.id
  
  # Update the user
  Given path '/api/users/' + userId
  And request { username: 'bobupdated', email: 'bob.updated@example.com', firstName: 'Bob', lastName: 'Updated' }
  When method put
  Then status 200
  And match response == { id: '#(userId)', username: 'bobupdated', email: 'bob.updated@example.com', firstName: 'Bob', lastName: 'Updated', createdAt: '#present', updatedAt: '#present' }

Scenario: Delete user
  # First create a user
  Given path '/api/users'
  And request { username: 'alicebrown', email: 'alice.brown@example.com', firstName: 'Alice', lastName: 'Brown' }
  When method post
  Then status 201
  And def userId = response.id
  
  # Delete the user
  Given path '/api/users/' + userId
  When method delete
  Then status 200
  And match response contains { message: 'User deleted successfully' }
  
  # Verify user is deleted
  Given path '/api/users/' + userId
  When method get
  Then status 404
