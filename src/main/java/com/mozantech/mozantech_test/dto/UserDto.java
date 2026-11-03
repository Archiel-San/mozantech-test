package com.mozantech.mozantech_test.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

//Costumo usar record em versões de java mais recentes
@Data
public class UserDto {
    private Long id;
    @NotBlank
    private String name;
    @NotBlank @Email
    private String email;
}
