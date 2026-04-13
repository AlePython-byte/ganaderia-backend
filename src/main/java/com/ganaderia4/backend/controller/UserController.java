package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.UserCreateRequestDTO;
import com.ganaderia4.backend.dto.UserResponseDTO;
import com.ganaderia4.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public UserResponseDTO createUser(@Valid @RequestBody UserCreateRequestDTO requestDTO) {
        return userService.createUser(requestDTO);
    }

    @GetMapping
    public List<UserResponseDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public UserResponseDTO getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping("/active/{active}")
    public List<UserResponseDTO> getUsersByActive(@PathVariable Boolean active) {
        return userService.getUsersByActive(active);
    }
}