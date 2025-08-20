Feature: User API Testing

Background:
  * url baseUrl
  * header Accept = 'application/json'
  * header Content-Type = 'application/json'

Scenario: Create a new user
  Given path '/users'
  And request { name: 'John Doe', email: 'john.doe@example.com' }
  When method post
  Then status 201
  And match response == { id: '#present', name: 'John Doe', email: 'john.doe@example.com' }

Scenario: Get all users
  Given path '/users'
  When method get
  Then status 200
  And match response == '#array'

Scenario: Get user by ID
  # First create a user
  Given path '/users'
  And request { name: 'Jane Smith', email: 'jane.smith@example.com' }
  When method post
  Then status 201
  And def userId = response.id
  
  # Now get the user by ID
  Given path '/users/' + userId
  When method get
  Then status 200
  And match response == { id: '#(userId)', name: 'Jane Smith', email: 'jane.smith@example.com' }

Scenario: Update user
  # First create a user
  Given path '/users'
  And request { name: 'Bob Wilson', email: 'bob.wilson@example.com' }
  When method post
  Then status 201
  And def userId = response.id
  
  # Update the user
  Given path '/users/' + userId
  And request { name: 'Bob Updated', email: 'bob.updated@example.com' }
  When method put
  Then status 200
  And match response == { id: '#(userId)', name: 'Bob Updated', email: 'bob.updated@example.com' }

Scenario: Delete user
  # First create a user
  Given path '/users'
  And request { name: 'Alice Brown', email: 'alice.brown@example.com' }
  When method post
  Then status 201
  And def userId = response.id
  
  # Delete the user
  Given path '/users/' + userId
  When method delete
  Then status 204
  
  # Verify user is deleted
  Given path '/users/' + userId
  When method get
  Then status 404
