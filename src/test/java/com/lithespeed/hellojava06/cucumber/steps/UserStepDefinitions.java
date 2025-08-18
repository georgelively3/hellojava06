package com.lithespeed.hellojava06.cucumber.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lithespeed.hellojava06.entity.User;
import com.lithespeed.hellojava06.repository.UserRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class UserStepDefinitions {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private ResponseEntity<String> lastResponse;
    private User testUser;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api";
    }

    @Given("the system has no users")
    public void theSystemHasNoUsers() {
        userRepository.deleteAll();
    }

    @Given("the system has a user with username {string} and email {string}")
    public void theSystemHasAUserWithUsernameAndEmail(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        userRepository.save(user);
    }

    @Given("I have a user with username {string}, email {string}, first name {string}, and last name {string}")
    public void iHaveAUserWithDetails(String username, String email, String firstName, String lastName) {
        testUser = new User();
        testUser.setUsername(username);
        testUser.setEmail(email);
        testUser.setFirstName(firstName);
        testUser.setLastName(lastName);
    }

    @When("I create the user")
    public void iCreateTheUser() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String userJson = objectMapper.writeValueAsString(testUser);
        HttpEntity<String> request = new HttpEntity<>(userJson, headers);

        lastResponse = restTemplate.postForEntity(getBaseUrl() + "/users", request, String.class);
    }

    @When("I request all users")
    public void iRequestAllUsers() {
        lastResponse = restTemplate.getForEntity(getBaseUrl() + "/users", String.class);
    }

    @When("I request user with username {string}")
    public void iRequestUserWithUsername(String username) {
        lastResponse = restTemplate.getForEntity(getBaseUrl() + "/users/username/" + username, String.class);
    }

    @When("I request user with ID {long}")
    public void iRequestUserWithId(long id) {
        lastResponse = restTemplate.getForEntity(getBaseUrl() + "/users/" + id, String.class);
    }

    @When("I search for users with name {string}")
    public void iSearchForUsersWithName(String name) {
        lastResponse = restTemplate.getForEntity(getBaseUrl() + "/users/search?name=" + name, String.class);
    }

    @When("I update user with ID {long} to have email {string}")
    public void iUpdateUserWithIdToHaveEmail(long id, String newEmail) throws Exception {
        // First get the existing user
        ResponseEntity<String> getUserResponse = restTemplate.getForEntity(getBaseUrl() + "/users/" + id, String.class);

        if (getUserResponse.getStatusCode() == HttpStatus.OK) {
            @SuppressWarnings("unchecked")
            Map<String, Object> userData = objectMapper.readValue(getUserResponse.getBody(), Map.class);

            User updateUser = new User();
            updateUser.setUsername((String) userData.get("username"));
            updateUser.setEmail(newEmail);
            updateUser.setFirstName((String) userData.get("firstName"));
            updateUser.setLastName((String) userData.get("lastName"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String userJson = objectMapper.writeValueAsString(updateUser);
            HttpEntity<String> request = new HttpEntity<>(userJson, headers);

            lastResponse = restTemplate.exchange(getBaseUrl() + "/users/" + id, HttpMethod.PUT, request, String.class);
        }
    }

    @When("I delete user with ID {long}")
    public void iDeleteUserWithId(long id) {
        lastResponse = restTemplate.exchange(getBaseUrl() + "/users/" + id, HttpMethod.DELETE, null, String.class);
    }

    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int expectedStatus) {
        assertThat(lastResponse.getStatusCode().value()).isEqualTo(expectedStatus);
    }

    @Then("the response should contain {int} users")
    public void theResponseShouldContainUsers(int expectedCount) throws Exception {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> users = objectMapper.readValue(lastResponse.getBody(), List.class);
        assertThat(users).hasSize(expectedCount);
    }

    @Then("the response should contain user with username {string}")
    public void theResponseShouldContainUserWithUsername(String expectedUsername) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> user = objectMapper.readValue(lastResponse.getBody(), Map.class);
        assertThat(user.get("username")).isEqualTo(expectedUsername);
    }

    @Then("the response should contain user with email {string}")
    public void theResponseShouldContainUserWithEmail(String expectedEmail) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> user = objectMapper.readValue(lastResponse.getBody(), Map.class);
        assertThat(user.get("email")).isEqualTo(expectedEmail);
    }

    @Then("the response should contain success message {string}")
    public void theResponseShouldContainSuccessMessage(String expectedMessage) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(lastResponse.getBody(), Map.class);
        assertThat(response.get("message")).isEqualTo(expectedMessage);
    }

    @Then("the user should be saved in the database")
    public void theUserShouldBeSavedInTheDatabase() {
        User savedUser = userRepository.findByUsername(testUser.getUsername()).orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(savedUser.getFirstName()).isEqualTo(testUser.getFirstName());
        assertThat(savedUser.getLastName()).isEqualTo(testUser.getLastName());
    }

    @Then("the user should be removed from the database")
    public void theUserShouldBeRemovedFromTheDatabase() {
        List<User> allUsers = userRepository.findAll();
        assertThat(allUsers).isEmpty();
    }

    @Then("the user count should be {int}")
    public void theUserCountShouldBe(int expectedCount) {
        long actualCount = userRepository.count();
        assertThat(actualCount).isEqualTo(expectedCount);
    }
}
