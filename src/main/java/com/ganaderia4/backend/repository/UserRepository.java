package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import com.ganaderia4.backend.model.Role;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByActive(Boolean active);

    Page<User> findByActive(Boolean active, Pageable pageable);

    List<User> findByActiveTrueAndRoleIn(List<Role> roles);
}
