package com.mozantech.mozantech_test.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockMovementResponse {
    private Long id;
    private LocalDateTime creationDate;
    private Long itemId;
    private Integer quantity;
    private Long orderId;
}
