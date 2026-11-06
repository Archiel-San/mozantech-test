package com.mozantech.mozantech_test.controller;

import com.mozantech.mozantech_test.dto.UserDto;
import com.mozantech.mozantech_test.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/users") @RequiredArgsConstructor
public class UserController {

    private final UserService service;

    // TODO uma vez que implementou-se o método de criação do usuário pelo keycloak, já não será utilizado este porque este cria o user sem keycloakId
//    @PostMapping
//    @ResponseStatus(HttpStatus.CREATED)
//    public UserDto create(@Valid @RequestBody UserDto d) {
//        return service.create(d);
//    }

    @GetMapping
    public List<UserDto> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public UserDto get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UserDto d) {
        return service.update(id, d);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

