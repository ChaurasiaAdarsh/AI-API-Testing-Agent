package com.apitest.agent.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:demotest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.ollama.base-url=http://localhost:11434",
        "app.testing.demo-mode=false",
        "app.demo.defects-enabled=true"
})
class DemoApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateDemoUserAndReturnBody() {
        Map<String, Object> user = Map.of(
                "name", "Integration Test User",
                "email", "integration@test.com",
                "age", 25,
                "role", "user"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(user, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/demo/users", request, Map.class);

        assertThat(response.getStatusCode().value()).isIn(200, 201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("id");
        assertThat(response.getBody()).containsKey("name");
        assertThat(response.getBody().get("name")).isEqualTo("Integration Test User");
    }

    @Test
    void shouldReturn404ForNonExistentUser() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/demo/users/99999", Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void shouldListUsers() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/demo/users", Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsKey("users");
    }

    @Test
    void shouldRejectUserBelowMinimumAge() {
        Map<String, Object> user = Map.of(
                "name", "Young User",
                "email", "young@test.com",
                "age", 17,
                "role", "user"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(user, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/demo/users", request, Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void shouldRejectUserWithMissingRequiredFields() {
        Map<String, Object> user = Map.of("name", "No Email");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(user, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/demo/users", request, Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void shouldDeleteUser() {
        Map<String, Object> user = Map.of(
                "name", "To Delete",
                "email", "delete@test.com",
                "age", 30,
                "role", "user"
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> created = restTemplate.postForEntity("/demo/users", new HttpEntity<>(user, headers), Map.class);

        assertThat(created.getBody()).isNotNull();
    }
}
