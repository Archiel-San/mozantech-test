package com.mozantech.mozantech_test.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RefreshTokenRequest {
    @NotBlank
    String refreshToken;
}
