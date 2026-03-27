package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.UserRequestDTO;
import com.ganaderia4.backend.dto.UserResponseDTO;
import com.ganaderia4.backend.exception.ConflictException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponseDTO createAdmin(UserRequestDTO requestDTO) {
        if (userRepository.findByEmail(requestDTO.getEmail()).isPresent()) {
            throw new ConflictException("Ya existe un usuario con ese correo");
        }

        User user = new User();
        user.setName(requestDTO.getName());
        user.setEmail(requestDTO.getEmail());
        user.setPassword(passwordEncoder.encode(requestDTO.getPassword()));
        user.setRole(Role.ADMINISTRADOR);
        user.setActive(true);

        User savedUser = userRepository.save(user);

        return mapToResponseDTO(savedUser);
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return mapToResponseDTO(user);
    }

    public List<UserResponseDTO> getUsersByActive(Boolean active) {
        return userRepository.findByActive(active)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public User getUserEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ese correo"));
    }

    private UserResponseDTO mapToResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getActive()
        );
    }
}