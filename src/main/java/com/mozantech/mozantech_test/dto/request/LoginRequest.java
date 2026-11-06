package com.mozantech.mozantech_test.dto.request;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@Builder
public class LoginRequest {
    @NotBlank
    String email;
    @NotBlank String password;
}
