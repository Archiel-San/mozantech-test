package com.mozantech.mozantech_test.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@AllArgsConstructor
public class OrderCompletedEvent {
    private final Long orderId;
    private final String userName;
    private final String userEmail;
    private final String itemName;
    private final Integer quantity;
}
