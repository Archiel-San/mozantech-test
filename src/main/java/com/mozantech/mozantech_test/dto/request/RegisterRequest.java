package com.mozantech.mozantech_test.dto.request;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RegisterRequest {

    @NotBlank
    @Email
    String email;
    @NotBlank @Size(min = 8) String password;
    @NotBlank String firstName;
}
