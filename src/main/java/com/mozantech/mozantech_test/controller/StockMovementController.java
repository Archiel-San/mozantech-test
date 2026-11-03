package com.mozantech.mozantech_test.controller;

import com.mozantech.mozantech_test.dto.request.QuantityRequest;
import com.mozantech.mozantech_test.dto.request.StockMovementRequest;
import com.mozantech.mozantech_test.dto.response.StockMovementResponse;
import com.mozantech.mozantech_test.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/stock-movements")
@RequiredArgsConstructor
public class StockMovementController {
    private final StockMovementService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockMovementResponse create(@Valid @RequestBody StockMovementRequest r) {
        return service.create(r);
    }

    @GetMapping
    public List<StockMovementResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public StockMovementResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public StockMovementResponse update(@PathVariable Long id, @Valid @RequestBody QuantityRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
