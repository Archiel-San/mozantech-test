package com.mozantech.mozantech_test.controller;

import com.mozantech.mozantech_test.dto.ItemDto;
import com.mozantech.mozantech_test.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemDto create(@Valid @RequestBody ItemDto d) {
        return service.create(d);
    }

    @GetMapping
    public List<ItemDto> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ItemDto get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public ItemDto update(@PathVariable Long id, @Valid @RequestBody ItemDto d) {
        return service.update(id, d);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
