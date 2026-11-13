package com.mozantech.mozantech_test.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class LoginResponse {

    String accessToken;
    String refreshToken;
    Long expiresIn;
}
