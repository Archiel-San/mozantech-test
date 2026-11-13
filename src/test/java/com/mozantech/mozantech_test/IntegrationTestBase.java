package com.mozantech.mozantech_test;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Starts the real application against a throw-away PostgreSQL container and talks to it over HTTP.
 * Requires Docker. Two things are replaced so the tests do not depend on Keycloak or SMTP:
 *  - JavaMailSender is a mock, so tests can assert on emails;
 *  - JwtDecoder is a mock that understands the fake tokens "admin-token" and "user-token"
 *    (anything else is rejected as an invalid token).
 * Every request is sent as the admin unless a test sets its own Authorization header.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class IntegrationTestBase {

    // One container shared by every test class (started once per JVM)
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /** Same value as the "sub" claim of the fake admin token, like a real Keycloak user id. */
    protected static final String KEYCLOAK_ID = "kc-ana";

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @MockBean
    protected JavaMailSender mailSender;

    @MockBean
    protected JwtDecoder jwtDecoder;

    protected Long userId;
    protected Long itemId;

    @BeforeEach
    void resetData() {
        doThrow(new BadJwtException("invalid token")).when(jwtDecoder).decode(anyString());
        doReturn(jwt("admin-token", KEYCLOAK_ID, "ADMIN", "USER")).when(jwtDecoder).decode("admin-token");
        doReturn(jwt("user-token", "kc-user", "USER")).when(jwtDecoder).decode("user-token");

        if (rest.getRestTemplate().getInterceptors().isEmpty()) {
            rest.getRestTemplate().getInterceptors().add((request, body, execution) -> {
                if (!request.getHeaders().containsKey("Authorization")) {
                    request.getHeaders().setBearerAuth("admin-token");
                }
                return execution.execute(request, body);
            });
        }

        jdbc.execute("TRUNCATE TABLE stock_movement, orders, item, usuario RESTART IDENTITY CASCADE");

        // POST /users no longer exists (users are created through Keycloak), so insert the user directly
        userId = jdbc.queryForObject(
                "INSERT INTO usuario (name, email, keycloak_id, created_at) VALUES (?, ?, ?, now()) RETURNING id",
                Long.class, "Ana", "ana@test.com", KEYCLOAK_ID);
        itemId = post("/items", body("name", "Keyboard")).get("id").asLong();
    }

    private static Jwt jwt(String token, String subject, String... roles) {
        Instant now = Instant.now();
        return Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", subject)
                .claim("realm_access", Collections.singletonMap("roles", Arrays.asList(roles)))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();
    }

    // ---------- helpers ----------

    protected static Map<String, Object> body(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    protected JsonNode post(String path, Map<String, Object> body) {
        ResponseEntity<JsonNode> response = rest.postForEntity(path, body, JsonNode.class);
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("POST %s returned %s: %s", path, response.getStatusCode(), response.getBody())
                .isTrue();
        return response.getBody();
    }

    protected ResponseEntity<JsonNode> put(String path, Map<String, Object> body) {
        return rest.exchange(path, HttpMethod.PUT, new HttpEntity<>(body), JsonNode.class);
    }

    protected JsonNode get(String path) {
        return rest.getForObject(path, JsonNode.class);
    }

    protected JsonNode newOrder(int quantity) {
        return post("/orders", body("itemId", itemId, "userId", userId, "quantity", quantity));
    }

    protected JsonNode newMovement(int quantity) {
        return post("/stock-movements", body("itemId", itemId, "quantity", quantity));
    }

    protected JsonNode order(long id) {
        return get("/orders/" + id);
    }

    /** Sum of stock-movement quantities that are (assigned=true) or are not (assigned=false) used by an order. */
    protected long sumMovements(boolean assigned) {
        long sum = 0;
        for (JsonNode movement : get("/stock-movements")) {
            boolean hasOrder = !movement.get("orderId").isNull();
            if (hasOrder == assigned) {
                sum += movement.get("quantity").asLong();
            }
        }
        return sum;
    }
}