package com.mozantech.mozantech_test.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ItemDto {
    private Long id;
    @NotBlank
    private String name;
}
