package com.mozantech.mozantech_test;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityIntegrationTest extends IntegrationTestBase {

    @LocalServerPort
    private int port;

    @Test
    void requestWithoutTokenIsRejected() {
        // a fresh client: it does not carry the admin token that the base class adds to `rest`
        ResponseEntity<String> response = new TestRestTemplate()
                .getForEntity("http://localhost:" + port + "/items", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void invalidTokenIsRejected() {
        assertThat(getWithToken("/items", "bad-token").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validTokenIsAccepted() {
        assertThat(getWithToken("/items", "user-token").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getWithToken("/orders", "admin-token").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<JsonNode> getWithToken(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
    }
}