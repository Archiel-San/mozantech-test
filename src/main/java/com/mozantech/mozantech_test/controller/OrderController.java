package com.mozantech.mozantech_test.controller;

import com.mozantech.mozantech_test.dto.request.OrderRequest;
import com.mozantech.mozantech_test.dto.request.QuantityRequest;
import com.mozantech.mozantech_test.dto.response.OrderResponse;
import com.mozantech.mozantech_test.dto.response.StockMovementResponse;
import com.mozantech.mozantech_test.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody OrderRequest r) {
        return service.create(r);
    }

    @GetMapping
    public List<OrderResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/stock-movements")
    public List<StockMovementResponse> movements(@PathVariable Long id) {
        return service.movementsOf(id);
    }

    @PutMapping("/{id}")
    public OrderResponse update(@PathVariable Long id, @Valid @RequestBody QuantityRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
