package com.mozantech.mozantech_test.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mozantech.mozantech_test.domain.User;
import com.mozantech.mozantech_test.dto.request.LoginRequest;
import com.mozantech.mozantech_test.dto.request.RefreshTokenRequest;
import com.mozantech.mozantech_test.dto.request.RegisterRequest;
import com.mozantech.mozantech_test.dto.response.LoginResponse;
import com.mozantech.mozantech_test.repository.UserRepository;

import javax.ws.rs.core.Response;

import lombok.extern.slf4j.Slf4j;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class AuthService {

    private final Keycloak keycloakAdminClient;
    private final UserRepository userRepository;

    public AuthService(
            Keycloak keycloakAdminClient,
            UserRepository userRepository
    ) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.userRepository = userRepository;
    }

    @Value("${kc.server-url}")
    private String serverUrl;

    @Value("${kc.realm}")
    private String realm;

    @Value("${kc.client-id}")
    private String clientId;

    @Value("${kc.client-secret}")
    private String clientSecret;


    // =========================================================
    // REGISTER
    // Creates user in Keycloak and PostgreSQL
    // =========================================================

    public void register(RegisterRequest request) {

        RealmResource realmResource = keycloakAdminClient.realm(realm);

        // Check PostgreSQL
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email Ja Em Uso"
            );
        }

        // Check Keycloak
        List<UserRepresentation> existing =
                realmResource.users().searchByEmail(request.getEmail(), true);

        if (!existing.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email Ja Em Uso"
            );
        }

        // Create Keycloak user
        UserRepresentation user = getUserRepresentation(request);

        Response response = realmResource.users().create(user);

        String keycloakUserId;

        try {
            if (response.getStatus() != 201) {

                log.error(
                        "Keycloak user creation failed: {}",
                        response.getStatus()
                );

                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to Create User"
                );
            }

            keycloakUserId = extractCreatedId(response);

        } finally {
            response.close();
        }

        assignRole(realmResource, keycloakUserId, "ROLE_USER");

        try {

            User applicationUser = User.builder()
                    .keycloakId(keycloakUserId)
                    .email(request.getEmail())
                    .name(request.getEmail())
                    .createdAt(LocalDateTime.now())
                    .build();

            userRepository.save(applicationUser);

            log.info(
                    "User successfully registered: {}",
                    keycloakUserId
            );

        } catch (Exception e) {

            log.error("PostgreSQL user registration failed", e);

            // Compensate: remove Keycloak user
            try {
                realmResource.users()
                        .get(keycloakUserId)
                        .remove();

                log.info(
                        "Keycloak user {} removed after database failure",
                        keycloakUserId
                );

            } catch (Exception rollbackException) {

                log.error(
                        "Failed to rollback Keycloak user: {}",
                        keycloakUserId,
                        rollbackException
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to save user"
            );
        }
    }


    private static UserRepresentation getUserRepresentation(
            RegisterRequest request
    ) {

        UserRepresentation user = new UserRepresentation();

        user.setUsername(request.getEmail());
        user.setEmail(request.getEmail());

        user.setEnabled(true);
        user.setEmailVerified(true);

        CredentialRepresentation credential =
                new CredentialRepresentation();

        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getPassword());
        credential.setTemporary(false);

        user.setCredentials(Collections.singletonList(credential));

        return user;
    }


    public LoginResponse login(LoginRequest loginRequest) {

        try (Keycloak userClient = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .grantType(OAuth2Constants.PASSWORD)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .username(loginRequest.getEmail())
                .password(loginRequest.getPassword())
                .build()) {

            AccessTokenResponse tokenResponse =
                    userClient.tokenManager().getAccessToken();

            return new LoginResponse(
                    tokenResponse.getToken(),
                    tokenResponse.getRefreshToken(),
                    tokenResponse.getExpiresIn()
            );

        } catch (Exception e) {

            log.warn(
                    "Login failed for {}: {}",
                    loginRequest.getEmail(),
                    e.getMessage()
            );

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid Credentials"
            );
        }
    }


    private String extractCreatedId(Response response) {

        String location = response.getHeaderString("Location");

        if (location == null || location.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Keycloak did not return the created user location"
            );
        }

        return location.substring(location.lastIndexOf("/") + 1);
    }


    private void assignRole(
            RealmResource realmResource,
            String userId,
            String roleName
    ) {

        try {

            RoleRepresentation role =
                    realmResource.roles()
                            .get(roleName)
                            .toRepresentation();

            realmResource.users()
                    .get(userId)
                    .roles()
                    .realmLevel()
                    .add(Collections.singletonList(role));
        } catch (Exception e) {

            log.warn(
                    "Could not assign role {}: {}",
                    roleName,
                    e.getMessage()
            );
        }
    }


    // =========================================================
    // REFRESH TOKEN
    // =========================================================

    public LoginResponse refresh(RefreshTokenRequest request) {

        try {

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_FORM_URLENCODED
            );

            MultiValueMap<String, String> body =
                    new LinkedMultiValueMap<>();

            body.add("grant_type", "refresh_token");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("refresh_token", request.getRefreshToken());

            HttpEntity<MultiValueMap<String, String>> entity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<JsonNode> response =
                    restTemplate.postForEntity(
                            serverUrl
                                    + "/realms/"
                                    + realm
                                    + "/protocol/openid-connect/token",
                            entity,
                            JsonNode.class
                    );

            JsonNode json = response.getBody();

            if (json == null
                    || !json.hasNonNull("access_token")
                    || !json.hasNonNull("expires_in")) {

                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid refresh token"
                );
            }

            String refreshToken = json.hasNonNull("refresh_token")
                    ? json.get("refresh_token").asText()
                    : request.getRefreshToken();

            return new LoginResponse(
                    json.get("access_token").asText(),
                    refreshToken,
                    json.get("expires_in").asLong()
            );

        } catch (ResponseStatusException e) {

            throw e;

        } catch (Exception e) {

            log.warn("Refresh failed: {}", e.getMessage());

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid refresh token"
            );
        }
    }
}