package com.mozantech.mozantech_test.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderResponse {
    private Long id;
    private LocalDateTime creationDate;
    private Long itemId;
    private String itemName;
    private Long userId;
    private Integer quantity;
    private long fulfilledQuantity;
    private double completionPercent;
    private boolean completed;
}
