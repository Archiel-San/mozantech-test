package com.mozantech.mozantech_test.controller;

import com.mozantech.mozantech_test.dto.request.LoginRequest;
import com.mozantech.mozantech_test.dto.request.RefreshTokenRequest;
import com.mozantech.mozantech_test.dto.request.RegisterRequest;
import com.mozantech.mozantech_test.dto.response.LoginResponse;
import com.mozantech.mozantech_test.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register (@RequestBody @Valid RegisterRequest request){
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login (@RequestBody @Valid LoginRequest loginRequest){
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    // AuthController.java
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @RequestBody @Valid RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

}
