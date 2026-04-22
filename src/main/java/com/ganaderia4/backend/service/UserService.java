package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.UserCreateRequestDTO;
import com.ganaderia4.backend.dto.UserRequestDTO;
import com.ganaderia4.backend.dto.UserResponseDTO;
import com.ganaderia4.backend.exception.ConflictException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "name", "email", "role", "active");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final PaginationService paginationService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuditLogService auditLogService,
                       PaginationService paginationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.paginationService = paginationService;
    }

    public UserResponseDTO createUser(UserCreateRequestDTO requestDTO) {
        return createUserInternal(
                requestDTO.getName(),
                requestDTO.getEmail(),
                requestDTO.getPassword(),
                requestDTO.getRole(),
                requestDTO.getActive() != null ? requestDTO.getActive() : true
        );
    }

    public UserResponseDTO createAdmin(UserRequestDTO requestDTO) {
        return createUserInternal(
                requestDTO.getName(),
                requestDTO.getEmail(),
                requestDTO.getPassword(),
                Role.ADMINISTRADOR,
                true
        );
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public Page<UserResponseDTO> getUsersPage(Boolean active, int page, int size, String sort, String direction) {
        PageRequest pageable = paginationService.createPageRequest(page, size, sort, direction, ALLOWED_SORT_FIELDS);

        Page<User> users = active != null
                ? userRepository.findByActive(active, pageable)
                : userRepository.findAll(pageable);

        return users.map(this::mapToResponseDTO);
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

    private UserResponseDTO createUserInternal(String name,
                                               String email,
                                               String rawPassword,
                                               Role role,
                                               Boolean active) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("Ya existe un usuario con ese correo");
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setActive(active != null ? active : true);

        User savedUser = userRepository.save(user);

        auditLogService.logWithCurrentActor(
                "CREATE_USER",
                "USER",
                savedUser.getId(),
                "API",
                "Creación de usuario " + savedUser.getEmail() + " con rol " + savedUser.getRole().name(),
                true
        );

        return mapToResponseDTO(savedUser);
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
