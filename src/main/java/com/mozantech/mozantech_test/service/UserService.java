package com.mozantech.mozantech_test.service;

import com.mozantech.mozantech_test.domain.Item;
import com.mozantech.mozantech_test.domain.User;
import com.mozantech.mozantech_test.dto.ItemDto;
import com.mozantech.mozantech_test.dto.UserDto;
import com.mozantech.mozantech_test.exception.ConflictException;
import com.mozantech.mozantech_test.exception.NotFoundException;
import com.mozantech.mozantech_test.repository.ItemRepository;
import com.mozantech.mozantech_test.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service @Slf4j @RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;

    public UserDto create(UserDto data) {
        if (userRepository.existsByEmail(data.getEmail()))
            throw new ConflictException("Email já se encontra registado: " + data.getEmail());
        User u = new User();
        u.setName(data.getName());
        u.setEmail(data.getEmail());
        return toDto(userRepository.save(u));
    }

    @Transactional(readOnly = true)
    public List<UserDto> findAll() { return userRepository.findAll().stream().map(this::toDto).collect(Collectors.toList()); }

    @Transactional(readOnly = true)
    public UserDto findById(Long id) { return toDto(get(id)); }

    public UserDto update(Long id, UserDto data) {
        User u = get(id);
        if (userRepository.existsByEmailAndIdNot(data.getEmail(), id))
            throw new ConflictException("Email já se encontra registado: " + data.getEmail());
        u.setName(data.getName());
        u.setEmail(data.getEmail());
        return toDto(u);
    }

    public void delete(Long id) { userRepository.delete(get(id)); userRepository.flush(); }

    private User get(Long id) { return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User " + id + " não encontrado")); }

    private UserDto toDto(User u) {
        UserDto d = new UserDto();
        d.setId(u.getId()); d.setName(u.getName()); d.setEmail(u.getEmail());
        return d;
    }
}


